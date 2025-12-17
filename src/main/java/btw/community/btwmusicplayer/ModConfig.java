package btw.community.btwmusicplayer;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ModConfig {

    public static final String DEFAULT_LOADING_MODE = "ALL";
    public static final String DEFAULT_SINGLE_PACK = "";
    public static final int DEFAULT_CONTEXT_DELAY = 7;
    public static final int DEFAULT_FADE_DURATION = 1000;
    public static final int DEFAULT_CAVE_Y = 60;

    public static final String COND_DIMENSION = "dimension";
    public static final String COND_BIOME = "biome";
    public static final String COND_TIME = "time_of_day";
    public static final String COND_COMBAT = "is_in_combat";
    public static final String COND_WEATHER = "weather";
    public static final String COND_CAVE = "is_in_cave";
    public static final String COND_BOSS = "boss_type";
    public static final String COND_VICTORY = "victory_after_boss";
    public static final String COND_MENU = "is_menu";
    public static final String COND_LOW_HEALTH = "is_low_health";

    public static final String[] ALL_CONDITIONS = {
            COND_DIMENSION, COND_BIOME, COND_TIME, COND_COMBAT,
            COND_WEATHER, COND_CAVE, COND_BOSS, COND_VICTORY, COND_MENU, COND_LOW_HEALTH
    };

    public String loadingMode = DEFAULT_LOADING_MODE;
    public String singleMusicPackName = DEFAULT_SINGLE_PACK;
    public int contextChangeDelaySeconds = DEFAULT_CONTEXT_DELAY;

    public int fadeDurationMs = DEFAULT_FADE_DURATION;
    public int caveYLevel = DEFAULT_CAVE_Y;

    public boolean enableDebugLogging = true;

    public Map<String, Boolean> conditionToggles = new HashMap<>();

    private static ModConfig instance;
    private final Path configFile;

    private ModConfig() {
        Path configDir = FabricLoader.getInstance().getGameDir().resolve("config");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            MusicLogger.error("Failed to create config directory: " + configDir);
        }
        this.configFile = configDir.resolve("btw-music-player.cfg");

        for (String cond : ALL_CONDITIONS) {
            conditionToggles.put(cond, true);
        }
    }

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
            instance.loadConfig();
        }
        return instance;
    }

    public void loadConfig() {
        Properties props = new Properties();
        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                props.load(reader);

                this.loadingMode = props.getProperty("musicpack_loading_mode", DEFAULT_LOADING_MODE);
                this.singleMusicPackName = props.getProperty("single_musicpack_name", DEFAULT_SINGLE_PACK);
                this.enableDebugLogging = Boolean.parseBoolean(props.getProperty("enable_debug_logging", "false"));

                this.contextChangeDelaySeconds = parseInt(props.getProperty("context_change_delay_seconds"), DEFAULT_CONTEXT_DELAY);
                this.fadeDurationMs = parseInt(props.getProperty("fade_duration_ms"), DEFAULT_FADE_DURATION);
                this.caveYLevel = parseInt(props.getProperty("cave_y_level"), DEFAULT_CAVE_Y);

                MusicLogger.always("Loading Condition Toggles:");
                for (String cond : ALL_CONDITIONS) {
                    String key = "condition_" + cond;
                    String valStr = props.getProperty(key, "true");
                    boolean val = Boolean.parseBoolean(valStr);
                    conditionToggles.put(cond, val);
                    MusicLogger.always(" -> " + cond + ": " + (val ? "ON" : "OFF"));
                }

                MusicLogger.always("Configuration loaded.");
                MusicLogger.always(" -> Mode: " + this.loadingMode);
                MusicLogger.always(" -> Fade Duration: " + this.fadeDurationMs + "ms");
                MusicLogger.always(" -> Cave Y Level: " + this.caveYLevel);

            } catch (IOException e) {
                MusicLogger.error("Error reading configuration file, using defaults. Error: " + e.getMessage());
            }
        } else {
            MusicLogger.always("Configuration file does not exist. Creating a default one.");
            saveConfig();
        }
    }

    public void saveConfig() {
        MusicLogger.log("Saving configuration to disk...");
        Properties props = new Properties();
        props.setProperty("musicpack_loading_mode", this.loadingMode);
        props.setProperty("single_musicpack_name", this.singleMusicPackName);
        props.setProperty("enable_debug_logging", String.valueOf(this.enableDebugLogging));
        props.setProperty("context_change_delay_seconds", String.valueOf(this.contextChangeDelaySeconds));
        props.setProperty("fade_duration_ms", String.valueOf(this.fadeDurationMs));
        props.setProperty("cave_y_level", String.valueOf(this.caveYLevel));

        for (Map.Entry<String, Boolean> entry : conditionToggles.entrySet()) {
            props.setProperty("condition_" + entry.getKey(), String.valueOf(entry.getValue()));
        }

        try (Writer writer = Files.newBufferedWriter(configFile)) {
            props.store(writer, "BTW Music Player Mod Configuration");
        } catch (IOException e) {
            MusicLogger.error("Configuration file write error. Error: " + e.getMessage());
        }
    }

    /**
     * Helper method to check if a specific condition is enabled globally.
     */
    public boolean isConditionEnabled(String conditionName) {
        return conditionToggles.getOrDefault(conditionName, true);
    }

    /**
     * Helper method to toggle a condition state.
     */
    public void setConditionEnabled(String conditionName, boolean enabled) {
        if (conditionToggles.containsKey(conditionName)) {
            conditionToggles.put(conditionName, enabled);
        } else {
            MusicLogger.error("Attempted to set unknown condition: " + conditionName);
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}