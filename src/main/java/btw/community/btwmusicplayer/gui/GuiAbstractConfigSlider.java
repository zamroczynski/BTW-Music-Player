package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;
import net.minecraft.src.GuiButton;
import net.minecraft.src.Minecraft;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public abstract class GuiAbstractConfigSlider extends GuiButton {
    protected float sliderValue;
    protected boolean dragging;
    protected final ModConfig config;

    public GuiAbstractConfigSlider(int id, int x, int y, int width, int height, ModConfig config) {
        super(id, x, y, width, height, "");
        this.config = config;
    }

    /**
     * Called when the slider value changes.
     * @param value Normalized value between 0.0 and 1.0
     */
    protected abstract void updateConfig(float value);

    /**
     * Called to update the button text based on current config/value.
     */
    protected abstract void updateDisplayString();

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

                updateConfig(this.sliderValue);
                updateDisplayString();
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

            updateConfig(this.sliderValue);
            updateDisplayString();

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