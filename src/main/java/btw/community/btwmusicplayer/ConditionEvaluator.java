package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongConditions;
import net.minecraft.src.*;

import java.util.List;

/**
 * Evaluates if a song's conditions match the current state of the game.
 * Updated to support deep trace logging for debugging music packs.
 */
public class ConditionEvaluator {

    public boolean check(SongConditions conditions, Minecraft mc, CombatTracker combatTracker) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return false;
        long worldTime = mc.theWorld.getTotalWorldTime();

        // 1. Victory Condition (Exclusive)
        if (conditions.victory_after_boss != null) {
            boolean active = combatTracker.isVictoryCooldownActive(worldTime);
            if (!active) {
                MusicLogger.trace("   -> Fail: Victory cooldown not active.");
            }
            return active;
        }

        // 2. Victory Cooldown Exclusion
        // If victory music is playing (cooldown active), suppress all other combat/boss music
        if (combatTracker.isVictoryCooldownActive(worldTime)) {
            if (conditions.is_in_combat != null || conditions.boss_type != null) {
                MusicLogger.trace("   -> Fail: Victory cooldown is active, suppressing combat/boss music.");
                return false;
            }
        }

        // 3. Boss Battle
        if (conditions.boss_type != null) {
            boolean isFightingBoss = false;
            List<Entity> nearbyEntities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(64.0, 64.0, 64.0));

            for (Entity entity : nearbyEntities) {
                if (entity.isEntityAlive()) {
                    if (conditions.boss_type.equalsIgnoreCase("wither") && entity instanceof EntityWither) {
                        isFightingBoss = true;
                        break;
                    } else if (conditions.boss_type.equalsIgnoreCase("ender_dragon") && entity instanceof EntityDragon) {
                        isFightingBoss = true;
                        break;
                    }
                }
            }
            if (!isFightingBoss) {
                MusicLogger.trace("   -> Fail: Boss '" + conditions.boss_type + "' not found nearby.");
                return false;
            }
        }

        // 4. Combat State
        if (conditions.is_in_combat != null) {
            boolean actualCombatState = combatTracker.isInCombat(worldTime);
            if (conditions.is_in_combat != actualCombatState) {
                MusicLogger.trace("   -> Fail: Combat state mismatch. Req: " + conditions.is_in_combat + ", Act: " + actualCombatState);
                return false;
            }
        }

        // 5. Dimension
        if (conditions.dimension != null) {
            int dimensionId = player.worldObj.provider.dimensionId;
            String dimensionName = (dimensionId == -1) ? "the_nether" : (dimensionId == 0) ? "overworld" : (dimensionId == 1) ? "the_end" : "unknown";
            if (!conditions.dimension.equalsIgnoreCase(dimensionName)) {
                MusicLogger.trace("   -> Fail: Dimension mismatch. Req: " + conditions.dimension + ", Act: " + dimensionName);
                return false;
            }
        }

        // 6. Cave / Underground
        if (conditions.is_in_cave != null) {
            boolean isBelowSeaLevel = player.posY < 60;
            boolean canSeeSky = player.worldObj.canBlockSeeTheSky((int)player.posX, (int)player.posY, (int)player.posZ);
            boolean isCave = isBelowSeaLevel && !canSeeSky;

            if (conditions.is_in_cave != isCave) {
                MusicLogger.trace("   -> Fail: Cave condition mismatch. Req: " + conditions.is_in_cave + ", Act: " + isCave);
                return false;
            }
        }

        // 7. Weather
        if (conditions.weather != null) {
            String currentWeather = player.worldObj.isThundering() ? "storm" : "clear";
            if (!conditions.weather.equalsIgnoreCase(currentWeather)) {
                MusicLogger.trace("   -> Fail: Weather mismatch. Req: " + conditions.weather + ", Act: " + currentWeather);
                return false;
            }
        }

        // 8. Time of Day
        if (conditions.time_of_day != null) {
            long time = player.worldObj.getWorldTime() % 24000;
            String timeName = (time >= 0 && time < 13000) ? "day" : "night";
            if (!conditions.time_of_day.equalsIgnoreCase(timeName)) {
                MusicLogger.trace("   -> Fail: Time mismatch. Req: " + conditions.time_of_day + ", Act: " + timeName);
                return false;
            }
        }

        // 9. Biome
        if (conditions.biome != null) {
            String biomeName = player.worldObj.getBiomeGenForCoords((int)player.posX, (int)player.posZ).biomeName;
            String normalizedBiomeName = biomeName.toLowerCase().replace(' ', '_');
            if (!conditions.biome.equalsIgnoreCase(normalizedBiomeName)) {
                MusicLogger.trace("   -> Fail: Biome mismatch. Req: " + conditions.biome + ", Act: " + normalizedBiomeName);
                return false;
            }
        }

        return true;
    }
}