package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;
import btw.community.btwmusicplayer.MusicLogger;
import btw.community.btwmusicplayer.MusicManager;
import net.minecraft.src.*;

public class GuiMusicPlayerConfig extends GuiScreen {
    private final GuiScreen parentScreen;
    private final ModConfig config;

    private GuiDelaySlider delaySlider;
    private GuiFadeSlider fadeSlider;
    private GuiCaveSlider caveSlider;
    private GuiButton debugButton;
    private GuiButton notificationButton;

    public GuiMusicPlayerConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.config = ModConfig.getInstance();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int x = this.width / 2 - 100;
        int yStart = this.height / 6;
        int spacing = 24;

        this.buttonList.add(new GuiButton(100, x, yStart, 200, 20, "Music Packs..."));
        this.buttonList.add(new GuiButton(105, x, yStart + spacing, 200, 20, "Song Conditions..."));

        this.delaySlider = new GuiDelaySlider(101, x, yStart + spacing * 2, 200, 20, config);
        this.buttonList.add(this.delaySlider);
        this.fadeSlider = new GuiFadeSlider(102, x, yStart + spacing * 3, 200, 20, config);
        this.buttonList.add(this.fadeSlider);
        this.caveSlider = new GuiCaveSlider(103, x, yStart + spacing * 4, 200, 20, config);
        this.buttonList.add(this.caveSlider);

        int halfWidth = 98;
        this.notificationButton = new GuiButton(106, x, yStart + spacing * 5, halfWidth, 20, getNotificationButtonText());
        this.buttonList.add(this.notificationButton);

        this.debugButton = new GuiButton(104, x + 102, yStart + spacing * 5, halfWidth, 20, getDebugButtonText());
        this.buttonList.add(this.debugButton);

        this.buttonList.add(new GuiButton(200, x, this.height - 40, 200, 20, I18n.getString("gui.done")));

        MusicLogger.log("[GUI] Config screen refreshed. Notifications: " + config.showNotifications);
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
            else if (button.id == 105) {
                this.mc.displayGuiScreen(new GuiSongConditionsConfig(this));
            }
            else if (button.id == 104) {
                config.enableDebugLogging = !config.enableDebugLogging;
                button.displayString = getDebugButtonText();
                MusicLogger.always("[GUI] Debug Logging set to: " + config.enableDebugLogging);
            }
            else if (button.id == 106) {
                config.showNotifications = !config.showNotifications;
                button.displayString = getNotificationButtonText();
                MusicLogger.always("[GUI] Notifications set to: " + config.showNotifications);
            }
        }
    }

    private String getNotificationButtonText() {
        return "Popups: " + (config.showNotifications ? "ON" : "OFF");
    }

    private String getDebugButtonText() {
        return "Debug: " + (config.enableDebugLogging ? "ON" : "OFF");
    }

    private void saveAndClose() {
        config.saveConfig();
        MusicManager.reload();
        this.mc.displayGuiScreen(this.parentScreen);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, "BTW Music Player Configuration", this.width / 2, 15, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}