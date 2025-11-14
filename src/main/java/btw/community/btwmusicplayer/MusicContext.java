package btw.community.btwmusicplayer;

/**
 * Encapsulates the player's state and game events relevant to the music system.
 * This instance-based context replaces the static MusicPlayerState class.
 */
public class MusicContext {
    private boolean attackTriggered = false;
    private boolean victorySignal = false;

    /**
     * Signals that the player has initiated or been involved in an attack.
     * This is consumed by the music system to potentially start combat music.
     */
    public void signalAttack() {
        this.attackTriggered = true;
    }

    /**
     * Checks if an attack was recently signaled and consumes the signal.
     * @return true if an attack was signaled since the last check, false otherwise.
     */
    public boolean consumeAttackSignal() {
        if (this.attackTriggered) {
            this.attackTriggered = false;
            return true;
        }
        return false;
    }

    /**
     * Signals that a boss has been defeated, triggering victory music logic.
     * Note: The boss type parameter from the original implementation is not currently used,
     * but can be re-added here if needed in the future.
     * @param bossType The type of boss defeated (e.g., "ender_dragon", "wither").
     */
    public void signalBossDefeated(String bossType) {
        this.victorySignal = true;
    }

    /**
     * Checks if a victory was recently signaled and consumes the signal.
     * @return true if a victory was signaled since the last check, false otherwise.
     */
    public boolean consumeVictorySignal() {
        if (this.victorySignal) {
            this.victorySignal = false;
            return true;
        }
        return false;
    }
}