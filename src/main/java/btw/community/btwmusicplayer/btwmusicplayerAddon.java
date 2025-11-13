package btw.community.btwmusicplayer;

import btw.AddonHandler;
import btw.BTWAddon;

public class btwmusicplayerAddon extends BTWAddon {
    private static btwmusicplayerAddon instance;

    public btwmusicplayerAddon() {
        super();
    }

    @Override
    public void initialize() {
        AddonHandler.logMessage(this.getName() + " Version " + this.getVersionString() + " Initializing...");

        MusicManager.load();

        System.out.println("Hello from BTW Music Player Addon!");
    }
}