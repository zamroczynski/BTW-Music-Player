package btw.community.btwmusicplayer;

import btw.AddonHandler;
import btw.BTWAddon;

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

        MusicManager.load();

        this.registerAddonCommand(new CommandMusicDebug());

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