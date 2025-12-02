package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.MusicLogger;
import btw.community.btwmusicplayer.gui.GuiButtonGear;
import btw.community.btwmusicplayer.gui.GuiMusicPlayerConfig;
import net.minecraft.src.EnumOptions;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiOptions;
import net.minecraft.src.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiOptions.class)
public abstract class GuiOptionsMixin extends GuiScreen {
    private static final int MUSIC_CONFIG_BUTTON_ID = 350;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void injectMusicConfigButton(CallbackInfo ci) {
        GuiButton musicSlider = null;
        int musicOptionId = EnumOptions.MUSIC.returnEnumOrdinal();

        for (Object obj : this.buttonList) {
            GuiButton button = (GuiButton) obj;
            if (button.id == musicOptionId) {
                musicSlider = button;
                break;
            }
        }

        if (musicSlider != null) {
            int oldWidth = musicSlider.width;
            int buttonSize = 20;
            int spacing = 4;

            musicSlider.width = oldWidth - (buttonSize + spacing);

            int gearX = musicSlider.xPosition + musicSlider.width + spacing;
            int gearY = musicSlider.yPosition;

            GuiButtonGear gearButton = new GuiButtonGear(MUSIC_CONFIG_BUTTON_ID, gearX, gearY);
            this.buttonList.add(gearButton);

            MusicLogger.log("Injected Config Button at " + gearX + "," + gearY);
        } else {
            MusicLogger.error("Could not find Music Slider to inject Config Button!");
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == MUSIC_CONFIG_BUTTON_ID) {
            this.mc.displayGuiScreen(new GuiMusicPlayerConfig(this));
            ci.cancel();
        }
    }
}