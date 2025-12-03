package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;

public class GuiFadeSlider extends GuiAbstractConfigSlider {

    private static final float MAX_VALUE_MS = 5000.0f;

    public GuiFadeSlider(int id, int x, int y, int width, int height, ModConfig config) {
        super(id, x, y, width, height, config);
        this.sliderValue = Math.min(MAX_VALUE_MS, Math.max(0, config.fadeDurationMs)) / MAX_VALUE_MS;
        updateDisplayString();
    }

    @Override
    protected void updateConfig(float value) {
        config.fadeDurationMs = (int)(value * MAX_VALUE_MS);
    }

    @Override
    protected void updateDisplayString() {
        int ms = config.fadeDurationMs;
        if (ms == 0) {
            this.displayString = "Crossfade: Cut (0ms)";
        } else {
            this.displayString = "Crossfade: " + ms + "ms";
        }
    }
}