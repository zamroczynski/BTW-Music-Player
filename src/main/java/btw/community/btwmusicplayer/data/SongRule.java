package btw.community.btwmusicplayer.data;

public class SongRule {
    public String file;
    public int priority;
    public SongConditions conditions;

    public transient String musicPackPath;
}