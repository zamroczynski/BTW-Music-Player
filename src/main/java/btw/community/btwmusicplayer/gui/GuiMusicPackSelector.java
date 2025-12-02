package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;
import btw.community.btwmusicplayer.MusicManager;
import btw.community.btwmusicplayer.data.MusicPackStatus;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.I18n;

import java.util.List;

public class GuiMusicPackSelector extends GuiScreen {
    private final GuiScreen parentScreen;
    private final ModConfig config;
    private GuiMusicPackList packList;
    private GuiButton modeButton;

    public GuiMusicPackSelector(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.config = ModConfig.getInstance();
    }

    @Override
    public void initGui() {
        List<MusicPackStatus> statuses = MusicManager.scanAvailablePacks();

        this.packList = new GuiMusicPackList(this, statuses);

        this.buttonList.clear();

        this.modeButton = new GuiButton(100, this.width / 2 - 155, this.height - 40, 150, 20, getModeButtonText());
        this.buttonList.add(this.modeButton);

        this.buttonList.add(new GuiButton(200, this.width / 2 + 5, this.height - 40, 150, 20, I18n.getString("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 200) {
                this.mc.displayGuiScreen(this.parentScreen);
            } else if (button.id == 100) {
                if (config.loadingMode.equals("ALL")) {
                    config.loadingMode = "SINGLE";
                } else {
                    config.loadingMode = "ALL";
                }
                this.modeButton.displayString = getModeButtonText();
            }
        }
    }

    public void selectMusicPack(String packName) {
        config.singleMusicPackName = packName;
    }

    public String getSelectedPackName() {
        return config.singleMusicPackName;
    }

    private String getModeButtonText() {
        return "Loading Mode: " + config.loadingMode;
    }

    public net.minecraft.src.Minecraft getMinecraft() {
        return this.mc;
    }

    public net.minecraft.src.FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.packList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, "Select Music Pack", this.width / 2, 15, 0xFFFFFF);

        if (config.loadingMode.equals("ALL")) {
            this.drawCenteredString(this.fontRenderer, "(Mode ALL: Logic combines songs from ALL valid packs)", this.width / 2, 28, 0xAAAAAA);
        } else {
            this.drawCenteredString(this.fontRenderer, "(Mode SINGLE: Logic uses ONLY the selected pack)", this.width / 2, 28, 0xAAAAAA);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}