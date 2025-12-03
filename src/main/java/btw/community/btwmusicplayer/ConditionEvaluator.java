package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongConditions;
import net.minecraft.src.*;

import java.util.List;
import java.util.Map;

/**
 * Evaluates if a song's conditions match the current state of the game.
 * Updated to support deep trace logging and Stateful Biome Logic (Memory).
 */
public class ConditionEvaluator {

    // Stores the last biome that was NOT a river or beach.
    private String lastSignificantBiome = null;

    /**
     * Updates the internal biome state. Should be called once per update cycle.
     */
    public void updateBiomeState(Minecraft mc, boolean trace) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return;

        // 1. Get Raw Biome
        String rawBiomeName = player.worldObj.getBiomeGenForCoords((int)player.posX, (int)player.posZ).biomeName;

        // 2. Normalize (lowercase, remove spaces)
        String currentBiome = rawBiomeName.toLowerCase().replace(' ', '_');

        // 3. Map Variants (Hills/Edges -> Parents)
        String mappedBiome = mapVariantToParent(currentBiome);

        // 4. Handle Transparent Biomes (Rivers/Beaches)
        if (isTransparentBiome(mappedBiome)) {
            // We are in a river/beach. Do NOT update lastSignificantBiome.
            // If the player logged in here, lastSignificantBiome remains null.
            if (trace) {
                 MusicLogger.trace("In transparent biome: " + mappedBiome + ". Keeping history: " + lastSignificantBiome);
            }
        } else {
            // We are in a solid biome. Update history.
            if (!mappedBiome.equals(lastSignificantBiome)) {
                if (trace) MusicLogger.trace("Biome Context Changed: " + lastSignificantBiome + " -> " + mappedBiome + " (Raw: " + rawBiomeName + ")");
                this.lastSignificantBiome = mappedBiome;
            }
        }
    }

    public boolean check(SongConditions conditions, Minecraft mc, CombatTracker combatTracker, Map<String, Integer> failureStats) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return false;
        long worldTime = mc.theWorld.getTotalWorldTime();

        // 1. Victory Condition
        if (conditions.victory_after_boss != null) {
            boolean active = combatTracker.isVictoryCooldownActive(worldTime);
            if (!active) {
                recordFailure(failureStats, "Victory cooldown not active");
            }
            return active;
        }

        // 2. Victory Cooldown Exclusion
        if (combatTracker.isVictoryCooldownActive(worldTime)) {
            if (conditions.is_in_combat != null || conditions.boss_type != null) {
                recordFailure(failureStats, "Victory cooldown active (suppressing combat/boss)");
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
                recordFailure(failureStats, "Boss not found: " + conditions.boss_type);
                return false;
            }
        }

        // 4. Combat State
        if (conditions.is_in_combat != null) {
            boolean actualCombatState = combatTracker.isInCombat(worldTime);
            if (conditions.is_in_combat != actualCombatState) {
                recordFailure(failureStats, "Combat state mismatch (Req: " + conditions.is_in_combat + ")");
                return false;
            }
        }

        // 5. Dimension
        if (conditions.dimension != null) {
            int dimensionId = player.worldObj.provider.dimensionId;
            String dimensionName = (dimensionId == -1) ? "the_nether" : (dimensionId == 0) ? "overworld" : (dimensionId == 1) ? "the_end" : "unknown";
            if (!conditions.dimension.equalsIgnoreCase(dimensionName)) {
                recordFailure(failureStats, "Dimension mismatch (Req: " + conditions.dimension + ")");
                return false;
            }
        }

        // 6. Cave / Underground
        if (conditions.is_in_cave != null) {
            int configCaveY = ModConfig.getInstance().caveYLevel;
            boolean isCave = player.posY < configCaveY;

            if (conditions.is_in_cave != isCave) {
                recordFailure(failureStats, "Cave condition mismatch (Req: " + conditions.is_in_cave + ")");
                return false;
            }
        }

        // 7. Weather
        if (conditions.weather != null) {
            String currentWeather = player.worldObj.isThundering() ? "storm" : "clear";
            if (!conditions.weather.equalsIgnoreCase(currentWeather)) {
                recordFailure(failureStats, "Weather mismatch (Req: " + conditions.weather + ")");
                return false;
            }
        }

        // 8. Time of Day
        if (conditions.time_of_day != null) {
            long time = player.worldObj.getWorldTime() % 24000;
            String timeName = (time >= 0 && time < 13000) ? "day" : "night";
            if (!conditions.time_of_day.equalsIgnoreCase(timeName)) {
                recordFailure(failureStats, "Time of day mismatch (Req: " + conditions.time_of_day + ")");
                return false;
            }
        }

        // 9. Biome (Smart Logic)
        if (conditions.biome != null) {
            // Requirement: Ignore rules that target rivers/beaches directly
            if (isTransparentBiome(conditions.biome)) {
                recordFailure(failureStats, "Rule ignored (Targets transparent biome: " + conditions.biome + ")");
                return false;
            }

            // If player logged in river/beach, we have no history.
            // Only allow non-biome specific songs (which this is NOT, since conditions.biome is not null).
            if (this.lastSignificantBiome == null) {
                recordFailure(failureStats, "No biome history (Player in river since login)");
                return false;
            }

            if (!conditions.biome.equalsIgnoreCase(this.lastSignificantBiome)) {
                recordFailure(failureStats, "Biome mismatch (Req: " + conditions.biome + ", Actual Context: " + this.lastSignificantBiome + ")");
                return false;
            }
        }

        return true;
    }

    private String mapVariantToParent(String biome) {
        if (biome.endsWith("hills")) {
            // desert_hills -> desert, forest_hills -> forest, etc.
            // "hills" is 5 chars.
            String parent = biome.substring(0, biome.length() - 5);
            // Handle underscore if present (e.g. desert_hills -> desert_)
            if (parent.endsWith("_")) {
                parent = parent.substring(0, parent.length() - 1);
            }
            return parent;
        }

        // Specific mapping for requested Edge and Shore cases
        if (biome.equals("extreme_hills_edge")) return "extreme_hills";
        if (biome.equals("mushroomislandshore")) return "mushroomisland";

        return biome;
    }

    private boolean isTransparentBiome(String biome) {
        return biome.equals("river") ||
                biome.equals("frozen_river") ||
                biome.equals("beach");
    }

    private void recordFailure(Map<String, Integer> stats, String reason) {
        if (stats != null) {
            stats.merge(reason, 1, Integer::sum);
        }
    }
}