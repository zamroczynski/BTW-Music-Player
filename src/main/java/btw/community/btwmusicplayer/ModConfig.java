package btw.community.btwmusicplayer;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {

    public static final String DEFAULT_LOADING_MODE = "ALL";
    public static final String DEFAULT_SINGLE_PACK = "";
    public static final int DEFAULT_CONTEXT_DELAY = 7;
    public static final int DEFAULT_FADE_DURATION = 1000;
    public static final int DEFAULT_CAVE_Y = 60;

    public String loadingMode = DEFAULT_LOADING_MODE;
    public String singleMusicPackName = DEFAULT_SINGLE_PACK;
    public int contextChangeDelaySeconds = DEFAULT_CONTEXT_DELAY;

    public int fadeDurationMs = DEFAULT_FADE_DURATION;
    public int caveYLevel = DEFAULT_CAVE_Y;

    public boolean enableDebugLogging = true; // TODO change to false on 1.0.0 version

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

        try (Writer writer = Files.newBufferedWriter(configFile)) {
            props.store(writer, "BTW Music Player Mod Configuration");
        } catch (IOException e) {
            MusicLogger.error("Configuration file write error. Error: " + e.getMessage());
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