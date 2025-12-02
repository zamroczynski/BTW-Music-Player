package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;
import btw.community.btwmusicplayer.MusicManager;
import net.minecraft.src.*;

public class GuiMusicPlayerConfig extends GuiScreen {
    private final GuiScreen parentScreen;
    private final ModConfig config;

    private GuiDelaySlider delaySlider;
    private GuiButton debugButton;

    public GuiMusicPlayerConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.config = ModConfig.getInstance();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int x = this.width / 2 - 100;
        int y = this.height / 4;

        this.buttonList.add(new GuiButton(100, x, y, 200, 20, "Music Packs..."));

        this.delaySlider = new GuiDelaySlider(101, x, y + 25, 200, 20, config);
        this.buttonList.add(this.delaySlider);

        this.debugButton = new GuiButton(102, x, y + 50, 200, 20, getDebugButtonText());
        this.buttonList.add(this.debugButton);

        this.buttonList.add(new GuiButton(200, x, this.height - 40, 200, 20, I18n.getString("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 200) {
                saveAndClose();
            }
            else if (button.id == 100) {
                this.mc.displayGuiScreen(new GuiMusicPackSelector(this));
            }
            else if (button.id == 102) {
                config.enableDebugLogging = !config.enableDebugLogging;
                this.debugButton.displayString = getDebugButtonText();
            }
        }
    }

    private String getDebugButtonText() {
        String state = config.enableDebugLogging ? "ON" : "OFF";
        return "Debug Logging: " + state;
    }

    private void saveAndClose() {
        config.saveConfig();
        MusicManager.reload();
        this.mc.displayGuiScreen(this.parentScreen);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, "BTW Music Player Configuration", this.width / 2, 20, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}