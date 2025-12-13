package btw.community.btwmusicplayer;

import btw.entity.mob.BTWSquidEntity;
import net.minecraft.src.*;

import java.util.List;

/**
 * Manages the combat state for the music player.
 * It tracks player-initiated attacks, threats, and boss battles to determine if combat music should play.
 * It also handles the post-boss-fight victory cooldown.
 */
public class MusicCombatTracker {
    private static final int COMBAT_THREAT_RANGE = 16;
    private static final int BOSS_PRESENCE_RANGE = 64;
    private static final long COMBAT_TIMEOUT_TICKS = 100; // 5 seconds
    private static final long VICTORY_COOLDOWN_TICKS = 500; // 25 seconds
    private static final int DIMENSION_CHANGE_GRACE_TICKS = 100; // 5 seconds of safety after portal

    private long lastCombatEventTick = -1;
    private long victoryCooldownEndTick = -1;
    private boolean hasLoggedCooldownEnd = true;

    private int gracePeriodTicks = 0;

    public MusicCombatTracker() {
        MusicLogger.log("[MusicCombatTracker] Initialized and ready.");
    }

    /**
     * Resets internal state. Called when changing dimensions or reloading config.
     */
    public void resetState() {
        this.lastCombatEventTick = -1;
        this.victoryCooldownEndTick = -1;
        this.hasLoggedCooldownEnd = true;
        this.gracePeriodTicks = DIMENSION_CHANGE_GRACE_TICKS;
        MusicLogger.log("[MusicCombatTracker] State reset. Grace period enabled for " + DIMENSION_CHANGE_GRACE_TICKS + " ticks.");
    }

    public void update(Minecraft mc, MusicContext musicContext) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return;

        if (this.gracePeriodTicks > 0) {
            this.gracePeriodTicks--;
            musicContext.consumeAttackSignal();
            musicContext.consumeVictorySignal();
            return;
        }

        long worldTime = mc.theWorld.getTotalWorldTime();

        // Check for victory signal first
        if (musicContext.consumeVictorySignal()) {
            MusicLogger.log("[MusicCombatTracker] Victory signal received. Starting cooldown.");
            this.victoryCooldownEndTick = worldTime + VICTORY_COOLDOWN_TICKS;
            this.lastCombatEventTick = -1; // End combat immediately
            this.hasLoggedCooldownEnd = false;
            musicContext.consumeAttackSignal(); // Clear any pending attack signals
            return;
        }

        // If in victory cooldown, do nothing else
        if (isVictoryCooldownActive(worldTime)) {
            return;
        }

        if (!hasLoggedCooldownEnd) {
            MusicLogger.log("[MusicCombatTracker] Victory cooldown complete. Resuming normal checks.");
            hasLoggedCooldownEnd = true;
        }

        boolean combatEventDetected = false;
        String reason = "None";

        // Priority 1: Boss Presence
        if (isBossPresent(player, BOSS_PRESENCE_RANGE)) {
            combatEventDetected = true;
            reason = "Boss Presence";
        } else {
            // Priority 2: Standard Combat Triggers & Sustainers
            boolean isThreatened = isThreatNearby(player, COMBAT_THREAT_RANGE);

            // Triggers (starts combat)
            if (player.hurtTime > 0 && isThreatened) {
                combatEventDetected = true;
                reason = "Player Hurt by Nearby Threat";
            } else if (musicContext.consumeAttackSignal()) {
                combatEventDetected = true;
                reason = "Player Initiated Attack";
            } else if (player.riddenByEntity instanceof BTWSquidEntity && player.riddenByEntity.isEntityAlive()) {
                combatEventDetected = true;
                reason = "Squid Headcrab";
            }
            else if (isInCombat(worldTime) && isThreatened) {
                combatEventDetected = true;
                reason = "Sustained by Threat Presence";
            }
        }

        if (combatEventDetected) {
            if (lastCombatEventTick < worldTime - 5) {
                MusicLogger.log("[MusicCombatTracker] Combat event detected. Reason: " + reason);
            }
            lastCombatEventTick = worldTime;
        }
    }

    public boolean isInCombat(long worldTime) {
        if (lastCombatEventTick <= 0) return false;
        if (worldTime < lastCombatEventTick) {
            lastCombatEventTick = worldTime;
            return false;
        }
        return (worldTime - lastCombatEventTick) < COMBAT_TIMEOUT_TICKS;
    }

    public boolean isVictoryCooldownActive(long worldTime) {
        return worldTime <= victoryCooldownEndTick;
    }

    private boolean isBossPresent(EntityPlayer player, double range) {
        List<Entity> nearbyEntities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(range, range, range));
        for (Entity entity : nearbyEntities) {
            if (entity.isEntityAlive() && entity instanceof IBossDisplayData) {
                return true;
            }
        }
        return false;
    }

    private boolean isThreatNearby(EntityPlayer player, double range) {
        List<Entity> nearbyThreats = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(range, 8.0, range));
        for (Entity entity : nearbyThreats) {
            if (entity.isEntityAlive() && (entity instanceof IMob || entity instanceof BTWSquidEntity || (entity instanceof EntityWolf && ((EntityWolf) entity).isAngry()))) {
                return true;
            }
        }
        return false;
    }
}