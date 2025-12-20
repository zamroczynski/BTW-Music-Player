package btw.community.btwmusicplayer;

import api.AddonHandler;

public class MusicLogger {

    private static final String LOG_PREFIX = "[BTWMusicPlayer] ";

    public static void log(String message) {
        if (ModConfig.getInstance() != null && !ModConfig.getInstance().enableDebugLogging) {
            return;
        }
        AddonHandler.logMessage(LOG_PREFIX + message);
    }

    public static void trace(String message) {
        if (ModConfig.getInstance() != null && !ModConfig.getInstance().enableDebugLogging) {
            return;
        }
        AddonHandler.logMessage(LOG_PREFIX + "[TRACE] " + message);
    }

    public static void error(String message) {
        AddonHandler.logMessage(LOG_PREFIX + "[ERROR] " + message);
    }

    public static void always(String message) {
        AddonHandler.logMessage(LOG_PREFIX + message);
    }
}