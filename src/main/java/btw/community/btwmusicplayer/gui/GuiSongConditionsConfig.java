package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.ModConfig;
import btw.community.btwmusicplayer.MusicLogger;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.I18n;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class GuiSongConditionsConfig extends GuiScreen {
    private final GuiScreen parentScreen;
    private final ModConfig config;

    private final Map<Integer, String> buttonIdToConditionMap = new HashMap<>();

    public GuiSongConditionsConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.config = ModConfig.getInstance();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonIdToConditionMap.clear();

        int titleHeight = 30;
        int buttonHeight = 20;
        int padding = 4;
        int colWidth = 150;
        int midX = this.width / 2;

        String[] conditions = ModConfig.ALL_CONDITIONS;

        for (int i = 0; i < conditions.length; i++) {
            String conditionKey = conditions[i];
            int buttonId = i;

            boolean isLeftCol = (i % 2 == 0);
            int x = isLeftCol ? (midX - colWidth - padding) : (midX + padding);
            int y = titleHeight + (i / 2) * (buttonHeight + padding);

            this.buttonIdToConditionMap.put(buttonId, conditionKey);

            this.buttonList.add(new GuiButton(buttonId, x, y, colWidth, buttonHeight, getButtonText(conditionKey)));
        }

        this.buttonList.add(new GuiButton(200, midX - 100, this.height - 40, 200, 20, I18n.getString("gui.done")));

        MusicLogger.log("[GUI] Opened Song Conditions Config. Loaded " + conditions.length + " toggles.");
    }

    private String getButtonText(String conditionKey) {
        boolean isEnabled = config.isConditionEnabled(conditionKey);
        String color = isEnabled ? "§a" : "§c";
        String stateText = isEnabled ? "ON" : "OFF";

        String prettyName = conditionKey.replace("_", " ");
        prettyName = Character.toUpperCase(prettyName.charAt(0)) + prettyName.substring(1);

        return prettyName + ": " + color + stateText;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 200) {
                this.mc.displayGuiScreen(this.parentScreen);
            }
            else if (buttonIdToConditionMap.containsKey(button.id)) {
                String conditionKey = buttonIdToConditionMap.get(button.id);
                boolean currentState = config.isConditionEnabled(conditionKey);
                boolean newState = !currentState;

                config.setConditionEnabled(conditionKey, newState);

                button.displayString = getButtonText(conditionKey);

                MusicLogger.log("[GUI] Toggled condition '" + conditionKey + "' to " + newState);
            }
        }
    }

    @Override
    protected void keyTyped(char par1, int par2) {
        if (par2 == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, "Active Song Conditions", this.width / 2, 10, 0xFFFFFF);

        this.drawCenteredString(this.fontRenderer, "§7Disable conditions to prevent songs requiring them from playing.", this.width / 2, this.height - 55, 0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}