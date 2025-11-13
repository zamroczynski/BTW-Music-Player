package btw.community.btwmusicplayer;

import btw.AddonHandler;

public class MusicLogger {

    private static final String LOG_PREFIX = "[BTWMusicPlayer] ";

    public static void log(String message) {
        if (!ModConfig.getInstance().enableDebugLogging) {
            return;
        }
        AddonHandler.logMessage(LOG_PREFIX + message);
    }

    public static void error(String message) {
        AddonHandler.logMessage(LOG_PREFIX + "[ERROR] " + message);
    }

    public static void always(String message) {
        AddonHandler.logMessage(LOG_PREFIX + message);
    }
}