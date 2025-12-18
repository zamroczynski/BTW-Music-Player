package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import java.io.File;

public class MusicNotificationManager {
    public static final long SLIDE_IN_MS = 500;
    public static final long DISPLAY_MS = 3000;
    public static final long SLIDE_OUT_MS = 500;
    public static final long TOTAL_DURATION = SLIDE_IN_MS + DISPLAY_MS + SLIDE_OUT_MS;

    private String currentTitle = "";
    private String currentPackName = "";
    private long startTime = -1;

    public MusicNotificationManager() {
        MusicLogger.always("[NotificationManager] Initialized.");
    }

    public void triggerNotification(SongRule rule) {
        if (rule == null) {
            MusicLogger.log("[NotificationManager] Triggered with null rule. Clearing notification.");
            this.startTime = -1;
            return;
        }

        if (!ModConfig.getInstance().showNotifications) {
            MusicLogger.log("[NotificationManager] Notifications are disabled in config. Skipping.");
            return;
        }

        if (rule.title != null && !rule.title.trim().isEmpty()) {
            this.currentTitle = rule.title;
        } else {
            this.currentTitle = rule.file;
        }

        try {
            File folder = new File(rule.musicPackPath);
            this.currentPackName = folder.getName();
        } catch (Exception e) {
            this.currentPackName = "Unknown Pack";
            MusicLogger.error("[NotificationManager] Failed to get pack name from path: " + rule.musicPackPath);
        }

        this.startTime = System.currentTimeMillis();

        MusicLogger.log("[NotificationManager] NEW NOTIFICATION: '" + currentTitle + "' from pack '" + currentPackName + "'");
        MusicLogger.trace("[NotificationManager] StartTime set to: " + this.startTime);
    }

    public boolean isActive() {
        if (startTime == -1) return false;
        return (System.currentTimeMillis() - startTime) < TOTAL_DURATION;
    }

    public float getAnimationProgress() {
        if (!isActive()) return 0.0f;
        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed < SLIDE_IN_MS) {
            return (float) elapsed / SLIDE_IN_MS;
        } else if (elapsed < SLIDE_IN_MS + DISPLAY_MS) {
            return 1.0f;
        } else {
            long fadeOutElapsed = elapsed - (SLIDE_IN_MS + DISPLAY_MS);
            float progress = 1.0f - ((float) fadeOutElapsed / SLIDE_OUT_MS);
            return Math.max(0.0f, progress);
        }
    }

    public String getCurrentTitle() { return currentTitle; }
    public String getCurrentPackName() { return currentPackName; }
}