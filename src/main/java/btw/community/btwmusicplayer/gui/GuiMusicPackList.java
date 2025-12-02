package btw.community.btwmusicplayer.gui;

import btw.community.btwmusicplayer.data.MusicPackStatus;
import net.minecraft.src.GuiSlot;
import net.minecraft.src.Tessellator;

import java.util.List;

public class GuiMusicPackList extends GuiSlot {
    private final GuiMusicPackSelector parentScreen;
    private final List<MusicPackStatus> packs;

    public GuiMusicPackList(GuiMusicPackSelector parent, List<MusicPackStatus> packs) {
        super(parent.getMinecraft(), parent.width, parent.height, 32, parent.height - 50, 36);
        this.parentScreen = parent;
        this.packs = packs;
    }

    @Override
    protected int getSize() {
        return packs.size();
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick) {
        if (index >= 0 && index < packs.size()) {
            MusicPackStatus selected = packs.get(index);
            parentScreen.selectMusicPack(selected.folderName);
        }
    }

    @Override
    protected boolean isSelected(int index) {
        if (index >= 0 && index < packs.size()) {
            String current = parentScreen.getSelectedPackName();
            return current.equals(packs.get(index).folderName);
        }
        return false;
    }

    @Override
    protected void drawBackground() {
        parentScreen.drawDefaultBackground();
    }

    @Override
    protected void drawSlot(int index, int x, int y, int height, Tessellator tessellator) {
        MusicPackStatus pack = packs.get(index);

        int nameColor = pack.isValid ? 0x55FF55 : 0xFF5555;
        this.parentScreen.drawString(this.parentScreen.getFontRenderer(), pack.folderName, x + 2, y + 1, nameColor);

        String subText = pack.isValid ? pack.validationMessage : ("ERROR: " + pack.validationMessage);
        int subColor = pack.isValid ? 0x808080 : 0xAA0000;
        this.parentScreen.drawString(this.parentScreen.getFontRenderer(), subText, x + 2, y + 12, subColor);

        this.parentScreen.drawString(this.parentScreen.getFontRenderer(), pack.fullPath, x + 2, y + 22, 0x404040);
    }

    public int getListWidth() {
        return this.parentScreen.width;
    }
}