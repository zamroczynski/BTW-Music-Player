package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;

public class GuiDelaySlider extends GuiAbstractConfigSlider {

    private static final float MAX_VALUE = 30.0f;

    public GuiDelaySlider(int id, int x, int y, int width, int height, ModConfig config) {
        super(id, x, y, width, height, config);
        this.sliderValue = Math.min(MAX_VALUE, Math.max(0, config.contextChangeDelaySeconds)) / MAX_VALUE;
        updateDisplayString();
    }

    @Override
    protected void updateConfig(float value) {
        config.contextChangeDelaySeconds = (int)(value * MAX_VALUE);
    }

    @Override
    protected void updateDisplayString() {
        int seconds = config.contextChangeDelaySeconds;
        if (seconds == 0) {
            this.displayString = "Context Delay: Immediate";
        } else {
            this.displayString = "Context Delay: " + seconds + "s";
        }
    }
}