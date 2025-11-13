package btw.community.btwmusicplayer;

import net.minecraft.src.Minecraft;

public class MusicPlayerState {

    private static boolean attackTriggered = false;

    public static void setPlayerAttackedByMob() {
        attackTriggered = true;
    }

    public static boolean wasAttackJustTriggered() {
        if (attackTriggered) {
            attackTriggered = false;
            return true;
        }
        return false;
    }
}