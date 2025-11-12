package btw.community.btwmusicplayer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.src.Minecraft;
import java.io.*;
import java.util.Properties;

public class ModConfig {

    public static final String DEFAULT_LOADING_MODE = "ALL";
    public static final String DEFAULT_SINGLE_PACK = "";

    public String loadingMode = DEFAULT_LOADING_MODE;
    public String singlePackName = DEFAULT_SINGLE_PACK;

    private static ModConfig instance;
    private final File configFile;

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
                System.out.println("[ModConfig] Załadowano konfigurację. Tryb: " + this.loadingMode + ", Pack: " + this.singlePackName);
            } catch (IOException e) {
                System.err.println("[ModConfig] Błąd odczytu pliku konfiguracyjnego, używam domyślnych.");
                e.printStackTrace();
            }
        } else {
            System.out.println("[ModConfig] Plik konfiguracyjny nie istnieje. Tworzę domyślny.");
            saveConfig();
        }
    }

    public void saveConfig() {
        Properties props = new Properties();
        props.setProperty("soundpack_loading_mode", this.loadingMode);
        props.setProperty("single_soundpack_name", this.singlePackName);

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "BTW Music Player Mod Configuration");
        } catch (IOException e) {
            System.err.println("[ModConfig] Błąd zapisu pliku konfiguracyjnego.");
            e.printStackTrace();
        }
    }
}