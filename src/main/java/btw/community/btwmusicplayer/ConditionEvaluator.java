package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongConditions;
import net.minecraft.src.*;

import java.util.List;

/**
 * Evaluates if a song's conditions match the current state of the game.
 * This class centralizes all condition-checking logic.
 */
public class ConditionEvaluator {

    public boolean check(SongConditions conditions, Minecraft mc, CombatTracker combatTracker, boolean log) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return false;
        long worldTime = mc.theWorld.getTotalWorldTime();

        // Victory condition is exclusive
        if (conditions.victory_after_boss != null) {
            return combatTracker.isVictoryCooldownActive(worldTime);
        }

        // During victory cooldown, no other combat/boss music can play
        if (combatTracker.isVictoryCooldownActive(worldTime) && (conditions.is_in_combat != null || conditions.boss_type != null)) {
            return false;
        }

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
            if (!isFightingBoss) return false;
        }

        if (conditions.is_in_combat != null) {
            if (conditions.is_in_combat != combatTracker.isInCombat(worldTime)) return false;
        }

        if (conditions.dimension != null) {
            int dimensionId = player.worldObj.provider.dimensionId;
            String dimensionName = (dimensionId == -1) ? "the_nether" : (dimensionId == 0) ? "overworld" : (dimensionId == 1) ? "the_end" : "unknown";
            if (!conditions.dimension.equalsIgnoreCase(dimensionName)) return false;
        }

        if (conditions.is_in_cave != null) {
            boolean isBelowSeaLevel = player.posY < 60;
            boolean canSeeSky = player.worldObj.canBlockSeeTheSky((int)player.posX, (int)player.posY, (int)player.posZ);
            boolean isCave = isBelowSeaLevel && !canSeeSky;
            if (conditions.is_in_cave != isCave) return false;
        }

        if (conditions.weather != null) {
            String currentWeather = player.worldObj.isThundering() ? "storm" : "clear";
            if (!conditions.weather.equalsIgnoreCase(currentWeather)) return false;
        }

        if (conditions.time_of_day != null) {
            long time = player.worldObj.getWorldTime() % 24000;
            String timeName = (time >= 0 && time < 13000) ? "day" : "night";
            if (!conditions.time_of_day.equalsIgnoreCase(timeName)) return false;
        }

        if (conditions.biome != null) {
            String biomeName = player.worldObj.getBiomeGenForCoords((int)player.posX, (int)player.posZ).biomeName;
            String normalizedBiomeName = biomeName.toLowerCase().replace(' ', '_');
            if (!conditions.biome.equalsIgnoreCase(normalizedBiomeName)) return false;
        }

        return true;
    }
}