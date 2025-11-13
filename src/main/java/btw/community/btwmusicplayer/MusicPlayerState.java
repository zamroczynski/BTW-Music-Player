package btw.community.btwmusicplayer;

import net.minecraft.src.Minecraft;

public class MusicPlayerState {
    private static boolean attackTriggered = false;
    private static String bossDefeatedSignal = null;
    private static boolean victorySignal = false;

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

    public static void reportBossDefeated(String bossType) {
        victorySignal = true;
    }
    public static boolean consumeVictorySignalIfPresent() {
        if (victorySignal) {
            victorySignal = false;
            return true;
        }
        return false;
    }
}