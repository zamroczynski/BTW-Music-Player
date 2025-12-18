package btw.community.btwmusicplayer;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

public class MusicNotificationRenderer extends Gui {
    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation("btw-music-player", "icon.png");
    private final Minecraft mc;

    public MusicNotificationRenderer(Minecraft mc) {
        this.mc = mc;
    }

    public void render(MusicNotificationManager nm, ScaledResolution res) {
        if (nm == null || !nm.isActive()) return;

        float progress = nm.getAnimationProgress();
        FontRenderer fr = mc.fontRenderer;

        String title = nm.getCurrentTitle();
        String pack = nm.getCurrentPackName();

        int iconSize = 20;
        int padding = 6;
        int textWidth = Math.max(fr.getStringWidth(title), fr.getStringWidth(pack));
        int totalWidth = iconSize + (padding * 3) + textWidth;
        int height = 32;

        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();

        int xBase = screenWidth;
        int xCurrent = xBase - (int)(totalWidth * progress);
        int yCurrent = (screenHeight / 2) - (height / 2);

        drawRect(xCurrent, yCurrent, xCurrent + totalWidth, yCurrent + height, 0x80000000);

        drawRect(xCurrent, yCurrent, xCurrent + 1, yCurrent + height, 0xFFFFFF00);

        mc.getTextureManager().bindTexture(ICON_TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawFullTexture(xCurrent + padding, yCurrent + (height - iconSize) / 2, iconSize, iconSize);

        int textX = xCurrent + iconSize + (padding * 2);
        int textY = yCurrent + padding;

        fr.drawStringWithShadow(title, textX, textY, 0xFFFF00);
        fr.drawStringWithShadow(pack, textX, textY + 12, 0xAAAAAA);

        if (progress == 1.0f && mc.theWorld.getTotalWorldTime() % 100 == 0) {
            MusicLogger.trace("[Renderer] Drawing notification at X: " + xCurrent + " (Width: " + totalWidth + ")");
        }
    }

    private void drawFullTexture(int x, int y, int width, int height) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, this.zLevel, 0.0, 1.0);
        tessellator.addVertexWithUV(x + width, y + height, this.zLevel, 1.0, 1.0);
        tessellator.addVertexWithUV(x + width, y, this.zLevel, 1.0, 0.0);
        tessellator.addVertexWithUV(x, y, this.zLevel, 0.0, 0.0);
        tessellator.draw();
    }
}