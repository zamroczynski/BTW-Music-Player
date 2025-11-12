package btw.community.btwmusicplayer;

import net.minecraft.src.Minecraft;

public class MusicPlayerState {

    private static long lastPlayerDamageFromMobTick = -1;

    public static void setPlayerAttackedByMob() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.theWorld != null) {
            lastPlayerDamageFromMobTick = mc.theWorld.getTotalWorldTime();
        }
    }

    public static boolean isInCombat() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || lastPlayerDamageFromMobTick < 0) {
            return false;
        }

        long worldTime = mc.theWorld.getTotalWorldTime();
        long timeSinceAttack = worldTime - lastPlayerDamageFromMobTick;

        return timeSinceAttack < 200;
    }

    public static String getDebugInfo() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return "(no world)";
        return "(lastAttackTick: " + lastPlayerDamageFromMobTick +
                ", timeSinceAttack: " + (mc.theWorld.getTotalWorldTime() - lastPlayerDamageFromMobTick) + ")";
    }
}