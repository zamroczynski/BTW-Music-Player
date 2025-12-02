package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;
import net.minecraft.src.GuiButton;
import net.minecraft.src.Minecraft;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiDelaySlider extends GuiButton {
    private float sliderValue;
    public boolean dragging;
    private final ModConfig config;

    public GuiDelaySlider(int id, int x, int y, int width, int height, ModConfig config) {
        super(id, x, y, width, height, "");
        this.config = config;
        this.sliderValue = Math.min(30, Math.max(0, config.contextChangeDelaySeconds)) / 30.0f;
        this.updateDisplayString();
    }

    private void updateDisplayString() {
        int seconds = (int)(this.sliderValue * 30);
        if (seconds == 0) {
            this.displayString = "Context Delay: Immediate";
        } else {
            this.displayString = "Context Delay: " + seconds + "s";
        }
    }

    @Override
    public int getHoverState(boolean mouseOver) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.drawButton) {
            if (!Mouse.isButtonDown(0)) {
                this.dragging = false;
            }

            if (this.dragging) {
                this.sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);

                if (this.sliderValue < 0.0F) this.sliderValue = 0.0F;
                if (this.sliderValue > 1.0F) this.sliderValue = 1.0F;

                config.contextChangeDelaySeconds = (int)(this.sliderValue * 30);
                this.updateDisplayString();
            }

            mc.getTextureManager().bindTexture(buttonTextures);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.xPosition + (int)(this.sliderValue * (float)(this.width - 8)), this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(this.xPosition + (int)(this.sliderValue * (float)(this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            this.sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);

            if (this.sliderValue < 0.0F) this.sliderValue = 0.0F;
            if (this.sliderValue > 1.0F) this.sliderValue = 1.0F;

            config.contextChangeDelaySeconds = (int)(this.sliderValue * 30);
            this.updateDisplayString();

            this.dragging = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }
}