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

    public String loadingMode = DEFAULT_LOADING_MODE;
    public String singlePackName = DEFAULT_SINGLE_PACK;

    private static ModConfig instance;
    private final Path configFile;

    public boolean enableDebugLogging = true; // TODO change to false when publish addon

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
                this.loadingMode = props.getProperty("soundpack_loading_mode", DEFAULT_LOADING_MODE);
                this.singlePackName = props.getProperty("single_soundpack_name", DEFAULT_SINGLE_PACK);
                this.enableDebugLogging = Boolean.parseBoolean(props.getProperty("enable_debug_logging", "false"));
                MusicLogger.always("Configuration loaded. Mode: " + this.loadingMode + ", Pack: " + this.singlePackName);
            } catch (IOException e) {
                MusicLogger.error("Error reading configuration file, using defaults. Error: " + e.getMessage());
            }
        } else {
            MusicLogger.always("Configuration file does not exist. Creating a default one.");
            saveConfig();
        }
    }

    public void saveConfig() {
        Properties props = new Properties();
        props.setProperty("soundpack_loading_mode", this.loadingMode);
        props.setProperty("single_soundpack_name", this.singlePackName);
        props.setProperty("enable_debug_logging", String.valueOf(this.enableDebugLogging));

        try (Writer writer = Files.newBufferedWriter(configFile)) {
            props.store(writer, "BTW Music Player Mod Configuration");
        } catch (IOException e) {
            MusicLogger.error("Configuration file write error. Error: " + e.getMessage());
        }
    }
}