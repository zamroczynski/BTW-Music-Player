package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;
import btw.community.btwmusicplayer.MusicLogger;
import btw.community.btwmusicplayer.MusicManager;
import btw.community.btwmusicplayer.data.MusicPackStatus;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.I18n;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
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

        int startX = this.width / 2 - 155;

        this.modeButton = new GuiButton(100, startX, this.height - 40, 100, 20, getModeButtonText());
        this.buttonList.add(this.modeButton);

        this.buttonList.add(new GuiButton(101, startX + 105, this.height - 40, 100, 20, "Open Folder"));

        this.buttonList.add(new GuiButton(200, startX + 210, this.height - 40, 100, 20, I18n.getString("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 200) {
                this.mc.displayGuiScreen(this.parentScreen);
            }
            else if (button.id == 100) {
                if (config.loadingMode.equals("ALL")) {
                    config.loadingMode = "SINGLE";
                } else {
                    config.loadingMode = "ALL";
                }
                this.modeButton.displayString = getModeButtonText();
            }
            else if (button.id == 101) {
                openMusicPacksFolder();
            }
        }
    }

    private void openMusicPacksFolder() {
        File folder = FabricLoader.getInstance().getGameDir().resolve(MusicManager.ROOT_DIR_NAME).toFile();

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder);
            } else {
                if (os.contains("linux")) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", folder.getAbsolutePath()});
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", folder.getAbsolutePath()});
                } else if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"explorer", folder.getAbsolutePath()});
                } else {
                    MusicLogger.error("Cannot open folder: Desktop API not supported on this system.");
                }
            }
        } catch (IOException e) {
            MusicLogger.error("Failed to open musicpacks folder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void selectMusicPack(String packName) {
        config.singleMusicPackName = packName;
    }

    public String getSelectedPackName() {
        return config.singleMusicPackName;
    }

    private String getModeButtonText() {
        return "Mode: " + config.loadingMode;
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