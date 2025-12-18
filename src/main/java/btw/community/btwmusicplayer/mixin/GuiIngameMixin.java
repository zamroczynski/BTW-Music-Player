package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.MusicManager;
import btw.community.btwmusicplayer.MusicNotificationManager;
import btw.community.btwmusicplayer.MusicNotificationRenderer;
import btw.community.btwmusicplayer.btwmusicplayerAddon;
import net.minecraft.src.GuiIngame;
import net.minecraft.src.Minecraft;
import net.minecraft.src.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class GuiIngameMixin {
    @Shadow
    private Minecraft mc;

    @Inject(method = "renderGameOverlay", at = @At("RETURN"))
    private void onRenderGameOverlay(float par1, boolean par2, int par3, int par4, CallbackInfo ci) {
        if (this.mc.gameSettings.hideGUI) {
            return;
        }

        MusicNotificationManager nm = btwmusicplayerAddon.getMusicContext().getNotificationManager();

        if (nm != null && nm.isActive() && this.mc.sndManager != null) {
            if (this.mc.sndManager instanceof MusicManager.MusicSoundManager) {
                MusicManager.MusicSoundManager musicSnd = (MusicManager.MusicSoundManager) this.mc.sndManager;
                MusicNotificationRenderer renderer = musicSnd.getNotificationRenderer();

                if (renderer != null) {
                    ScaledResolution res = new ScaledResolution(this.mc.gameSettings, this.mc.displayWidth, this.mc.displayHeight);
                    renderer.render(nm, res);
                }
            }
        }
    }
}