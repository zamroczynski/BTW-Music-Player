package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.MusicLogger;
import net.minecraft.src.GuiButton;
import net.minecraft.src.Minecraft;
import net.minecraft.src.ResourceLocation;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;

public class GuiButtonGear extends GuiButton {
    private static final ResourceLocation GEAR_TEXTURE = new ResourceLocation("btw-music-player", "textures/gui/icon_gear.png");
    private boolean hasLoggedRender = false;

    public GuiButtonGear(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.drawButton) {
            mc.getTextureManager().bindTexture(buttonTextures);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            this.field_82253_i = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int hoverState = this.getHoverState(this.field_82253_i);

            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + hoverState * 20, this.width / 2, this.height);
            this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + hoverState * 20, this.width / 2, this.height);

            this.mouseDragged(mc, mouseX, mouseY);

            try {
                mc.getTextureManager().bindTexture(GEAR_TEXTURE);

                if (!hasLoggedRender) {
                    MusicLogger.log("[GUI] Rendering Gear Icon at: " + (this.xPosition + 2) + ", " + (this.yPosition + 2));
                    hasLoggedRender = true;
                }

                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

                drawFullTexture(this.xPosition + 2, this.yPosition + 1, 16, 16);

            } catch (Exception e) {
                MusicLogger.error("Failed to render gear icon: " + e.getMessage());
            }
        }
    }

    /**
     * Draws the entire currently bound texture (UV 0.0 - 1.0) in a specified rectangle.
     * This solves the drawTexturedModalRect problem, which assumes a 256x256 texture.
     */
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