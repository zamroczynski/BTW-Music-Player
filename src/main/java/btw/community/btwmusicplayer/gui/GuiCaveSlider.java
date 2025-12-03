package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;

public class GuiCaveSlider extends GuiAbstractConfigSlider {

    private static final float MAX_Y = 128.0f;

    public GuiCaveSlider(int id, int x, int y, int width, int height, ModConfig config) {
        super(id, x, y, width, height, config);
        this.sliderValue = Math.min(MAX_Y, Math.max(0, config.caveYLevel)) / MAX_Y;
        updateDisplayString();
    }

    @Override
    protected void updateConfig(float value) {
        config.caveYLevel = (int)(value * MAX_Y);
    }

    @Override
    protected void updateDisplayString() {
        this.displayString = "Cave Depth: Y < " + config.caveYLevel;
    }
}