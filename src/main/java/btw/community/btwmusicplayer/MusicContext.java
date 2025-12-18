package btw.community.btwmusicplayer;

/**
 * Encapsulates the player's state and game events relevant to the music system.
 * Acts as a bridge between the game logic (Mixin) and external tools (Commands/Debug).
 */
public class MusicContext {
    private boolean attackTriggered = false;
    private boolean victorySignal = false;
    private MusicCombatTracker combatTracker;
    private PlaylistManager playlistManager;
    private ConditionEvaluator conditionEvaluator;
    private OverlayManager overlayManager;
    private MusicNotificationManager notificationManager;

    public void registerComponents(MusicCombatTracker combatTracker, PlaylistManager playlistManager, ConditionEvaluator conditionEvaluator, OverlayManager overlayManager, MusicNotificationManager notificationManager) {
        this.combatTracker = combatTracker;
        this.playlistManager = playlistManager;
        this.conditionEvaluator = conditionEvaluator;
        this.overlayManager = overlayManager;
        this.notificationManager = notificationManager;
        MusicLogger.log("[MusicContext] Components registered for access.");
    }

    public MusicCombatTracker getCombatTracker() {
        return combatTracker;
    }

    public PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    public ConditionEvaluator getConditionEvaluator() {
        return conditionEvaluator;
    }

    public MusicNotificationManager getNotificationManager() {
        return notificationManager;
    }

    public void signalAttack() {
        this.attackTriggered = true;
    }

    public boolean consumeAttackSignal() {
        if (this.attackTriggered) {
            this.attackTriggered = false;
            return true;
        }
        return false;
    }

    public void signalBossDefeated(String bossType) {
        this.victorySignal = true;
    }

    public boolean consumeVictorySignal() {
        if (this.victorySignal) {
            this.victorySignal = false;
            return true;
        }
        return false;
    }

    public void registerComponents(MusicCombatTracker combatTracker, PlaylistManager playlistManager, ConditionEvaluator conditionEvaluator) {
        this.registerComponents(combatTracker, playlistManager, conditionEvaluator, null, null);
    }

    public OverlayManager getOverlayManager() {
        return overlayManager;
    }
}