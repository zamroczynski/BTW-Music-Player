package btw.community.btwmusicplayer;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.util.Properties;

public class ModConfig {

    public static final String DEFAULT_LOADING_MODE = "ALL";
    public static final String DEFAULT_SINGLE_PACK = "";

    public String loadingMode = DEFAULT_LOADING_MODE;
    public String singlePackName = DEFAULT_SINGLE_PACK;

    private static ModConfig instance;
    private final File configFile;

    public boolean enableDebugLogging = true; // TODO change to false when publish addon

    private ModConfig() {
        File configDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.configFile = new File(configDir, "btw-music-player.cfg");
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
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
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

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "BTW Music Player Mod Configuration");
        } catch (IOException e) {
            MusicLogger.error("Configuration file write error. Error: " + e.getMessage());
        }
    }
}