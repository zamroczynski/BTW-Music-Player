package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongConditions;
import net.minecraft.src.*;

import java.util.List;
import java.util.Map;

/**
 * Evaluates if a song's conditions match the current state of the game.
 * Updated to allow music in ALL menus when outside of a world.
 */
public class ConditionEvaluator {

    private String lastSignificantBiome = null;

    public void updateBiomeState(Minecraft mc, boolean trace) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) {
            return;
        }

        String rawBiomeName = player.worldObj.getBiomeGenForCoords((int)player.posX, (int)player.posZ).biomeName;
        String currentBiome = rawBiomeName.toLowerCase().replace(' ', '_');
        String mappedBiome = mapVariantToParent(currentBiome);

        if (isTransparentBiome(mappedBiome)) {
            if (trace) {
                MusicLogger.trace("In transparent biome: " + mappedBiome + ". Keeping history: " + lastSignificantBiome);
            }
        } else {
            if (!mappedBiome.equals(lastSignificantBiome)) {
                if (trace) MusicLogger.trace("Biome Context Changed: " + lastSignificantBiome + " -> " + mappedBiome + " (Raw: " + rawBiomeName + ")");
                this.lastSignificantBiome = mappedBiome;
            }
        }
    }

    public boolean check(SongConditions conditions, Minecraft mc, MusicCombatTracker combatTracker, Map<String, Integer> failureStats) {
        ModConfig config = ModConfig.getInstance();
        EntityClientPlayerMP player = mc.thePlayer;
        boolean hasWorld = (player != null && player.worldObj != null);
        long worldTime = hasWorld ? mc.theWorld.getTotalWorldTime() : 0;

        // --- 1. MENU CHECK ---
        if (conditions.is_menu != null) {
            if (!config.isConditionEnabled(ModConfig.COND_MENU)) {
                recordFailure(failureStats, "Global Toggle OFF: Menu");
                return false;
            }

            boolean isMenuContext = !hasWorld;

            if (conditions.is_menu != isMenuContext) {
                String screenName = (mc.currentScreen != null) ? mc.currentScreen.getClass().getSimpleName() : "null";
                recordFailure(failureStats, "Menu mismatch (Req: " + conditions.is_menu + ", Actual: " + isMenuContext + ", Screen: " + screenName + ")");
                return false;
            }
        }

        // --- 2. VICTORY CHECK ---
        if (conditions.victory_after_boss != null) {
            if (!config.isConditionEnabled(ModConfig.COND_VICTORY)) {
                recordFailure(failureStats, "Global Toggle OFF: Victory");
                return false;
            }
            if (!hasWorld) {
                recordFailure(failureStats, "No World (Victory)");
                return false;
            }
            boolean active = combatTracker.isVictoryCooldownActive(worldTime);
            if (!active) {
                recordFailure(failureStats, "Victory cooldown not active");
            }
            return active;
        }

        // Victory Cooldown Exclusion
        if (hasWorld && combatTracker.isVictoryCooldownActive(worldTime)) {
            if (conditions.is_in_combat != null || conditions.boss_type != null) {
                recordFailure(failureStats, "Victory cooldown active (suppressing combat/boss)");
                return false;
            }
        }

        // --- 3. BOSS CHECK ---
        if (conditions.boss_type != null) {
            if (!config.isConditionEnabled(ModConfig.COND_BOSS)) {
                recordFailure(failureStats, "Global Toggle OFF: Boss");
                return false;
            }
            if (!hasWorld) {
                recordFailure(failureStats, "No World (Boss)");
                return false;
            }

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

        // --- 4. COMBAT CHECK ---
        if (conditions.is_in_combat != null) {
            if (!config.isConditionEnabled(ModConfig.COND_COMBAT)) {
                recordFailure(failureStats, "Global Toggle OFF: Combat");
                return false;
            }
            if (!hasWorld) {
                recordFailure(failureStats, "No World (Combat)");
                return false;
            }

            boolean actualCombatState = combatTracker.isInCombat(worldTime);
            if (conditions.is_in_combat != actualCombatState) {
                recordFailure(failureStats, "Combat state mismatch (Req: " + conditions.is_in_combat + ")");
                return false;
            }
        }

        // --- 5. DIMENSION CHECK ---
        if (conditions.dimension != null) {
            if (!config.isConditionEnabled(ModConfig.COND_DIMENSION)) {
                recordFailure(failureStats, "Global Toggle OFF: Dimension");
                return false;
            }
            if (!hasWorld) {
                recordFailure(failureStats, "No World (Dimension)");
                return false;
            }

            int dimensionId = player.worldObj.provider.dimensionId;
            String dimensionName = (dimensionId == -1) ? "the_nether" : (dimensionId == 0) ? "overworld" : (dimensionId == 1) ? "the_end" : "unknown";
            if (!conditions.dimension.equalsIgnoreCase(dimensionName)) {
                recordFailure(failureStats, "Dimension mismatch (Req: " + conditions.dimension + ")");
                return false;
            }
        }

        // --- 6. CAVE CHECK ---
        if (conditions.is_in_cave != null) {
            if (!config.isConditionEnabled(ModConfig.COND_CAVE)) {
                recordFailure(failureStats, "Global Toggle OFF: Cave");
                return false;
            }
            if (!hasWorld) {
                recordFailure(failureStats, "No World (Cave)");
                return false;
            }

            int configCaveY = ModConfig.getInstance().caveYLevel;
            boolean isCave = player.posY < configCaveY;

            if (conditions.is_in_cave != isCave) {
                recordFailure(failureStats, "Cave condition mismatch (Req: " + conditions.is_in_cave + ")");
                return false;
            }
        }

        // --- 7. WEATHER CHECK ---
        if (conditions.weather != null) {
            if (!config.isConditionEnabled(ModConfig.COND_WEATHER)) {
                recordFailure(failureStats, "Global Toggle OFF: Weather");
                return false;
            }
            if (!hasWorld) {
                recordFailure(failureStats, "No World (Weather)");
                return false;
            }

            String currentWeather = player.worldObj.isThundering() ? "storm" : "clear";
            if (!conditions.weather.equalsIgnoreCase(currentWeather)) {
                recordFailure(failureStats, "Weather mismatch (Req: " + conditions.weather + ")");
                return false;
            }
        }

        // --- 8. TIME OF DAY CHECK ---
        if (conditions.time_of_day != null) {
            if (!config.isConditionEnabled(ModConfig.COND_TIME)) {
                recordFailure(failureStats, "Global Toggle OFF: Time");
                return false;
            }
            if (!hasWorld) {
                recordFailure(failureStats, "No World (Time)");
                return false;
            }

            long time = player.worldObj.getWorldTime() % 24000;
            String timeName = (time >= 0 && time < 13000) ? "day" : "night";
            if (!conditions.time_of_day.equalsIgnoreCase(timeName)) {
                recordFailure(failureStats, "Time of day mismatch (Req: " + conditions.time_of_day + ")");
                return false;
            }
        }

        // --- 9. BIOME CHECK ---
        if (conditions.biome != null) {
            if (!config.isConditionEnabled(ModConfig.COND_BIOME)) {
                recordFailure(failureStats, "Global Toggle OFF: Biome");
                return false;
            }
            if (!hasWorld) {
                recordFailure(failureStats, "No World (Biome)");
                return false;
            }

            if (isTransparentBiome(conditions.biome)) {
                recordFailure(failureStats, "Rule ignored (Targets transparent biome: " + conditions.biome + ")");
                return false;
            }

            if (this.lastSignificantBiome == null) {
                recordFailure(failureStats, "No biome history (Player in river since login)");
                return false;
            }

            if (!conditions.biome.equalsIgnoreCase(this.lastSignificantBiome)) {
                recordFailure(failureStats, "Biome mismatch (Req: " + conditions.biome + ", Actual Context: " + this.lastSignificantBiome + ")");
                return false;
            }
        }

        // --- 10. LOW HEALTH CHECK (Overlay Condition) ---
        if (conditions.is_low_health != null) {
            if (!config.isConditionEnabled(ModConfig.COND_LOW_HEALTH)) {
                return false;
            }
            if (!hasWorld) {
                return false;
            }

            float currentHealth = player.getHealth();
            boolean isPlayerLowHealth = currentHealth <= 10.0f && currentHealth > 0.0f;

            if (conditions.is_low_health != isPlayerLowHealth) {
                 recordFailure(failureStats, "Health mismatch (Req: " + conditions.is_low_health + ", Actual: " + isPlayerLowHealth + ", HP: " + currentHealth + ")");
                return false;
            } else {
                 MusicLogger.trace("Health Condition Match: HP=" + currentHealth);
            }
        }

        return true;
    }

    private String mapVariantToParent(String biome) {
        if (biome.endsWith("hills")) {
            String parent = biome.substring(0, biome.length() - 5);
            if (parent.endsWith("_")) {
                parent = parent.substring(0, parent.length() - 1);
            }
            return parent;
        }
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