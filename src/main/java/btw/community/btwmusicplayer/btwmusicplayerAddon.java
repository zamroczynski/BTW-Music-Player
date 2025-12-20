package btw.community.btwmusicplayer;

import api.AddonHandler;
import api.BTWAddon;

public class btwmusicplayerAddon extends BTWAddon {
    private static btwmusicplayerAddon instance;
    private static final MusicContext musicContext = new MusicContext();

    public btwmusicplayerAddon() {
        super();
        instance = this;
    }

    @Override
    public void initialize() {
        AddonHandler.logMessage(this.getName() + " Version " + this.getVersionString() + " Initializing...");

        try {
            MusicLogger.always("Initializing Config and Music Manager...");
            MusicManager.load();
        } catch (Exception e) {
            AddonHandler.logMessage("[CRITICAL ERROR] Failed to load MusicManager: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            this.registerAddonCommand(new CommandMusicDebug());
            MusicLogger.always("Commands registered successfully.");
        } catch (Exception e) {
            AddonHandler.logMessage("[CRITICAL ERROR] Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }

        MusicLogger.always("BTW Music Player Addon initialized successfully.");
    }

    /**
     * Provides global access to the single MusicContext instance.
     * @return The singleton MusicContext object.
     */
    public static MusicContext getMusicContext() {
        return musicContext;
    }
}