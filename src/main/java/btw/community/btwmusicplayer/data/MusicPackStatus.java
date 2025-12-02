package btw.community.btwmusicplayer.data;

public class MusicPackStatus {
    public final String folderName;
    public final String fullPath;
    public final boolean isValid;
    public final String validationMessage;

    public MusicPackStatus(String folderName, String fullPath, boolean isValid, String validationMessage) {
        this.folderName = folderName;
        this.fullPath = fullPath;
        this.isValid = isValid;
        this.validationMessage = validationMessage;
    }
}