package btw.community.btwmusicplayer.data;

public class SongRule {
    public String file;
    public String title;
    public int priority;
    public SongConditions conditions;
    public transient String musicPackPath;
    public transient boolean hasBeenPlayed = false;

    /**
     * Determines if this rule should be handled by the Overlay Manager (played on top of other music)
     * instead of the main Playlist Manager.
     */
    public boolean isOverlayRule() {
        if (conditions == null) return false;
        return conditions.is_low_health != null;
    }
}