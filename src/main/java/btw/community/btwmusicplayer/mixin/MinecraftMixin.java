package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.MusicLogger;
import net.minecraft.src.GuiMainMenu;
import net.minecraft.src.Minecraft;
import net.minecraft.src.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow public SoundManager sndManager;
    @Shadow private boolean isGamePaused;

    private long lastMusicUpdate = 0;


    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/SoundManager;setListener(Lnet/minecraft/src/EntityLivingBase;F)V"))
    private void onGameLoopSoundUpdate(CallbackInfo ci) {
        if (this.sndManager == null) return;

        long now = System.currentTimeMillis();

        if (now - lastMusicUpdate > 100) {
            lastMusicUpdate = now;
            this.sndManager.playRandomMusicIfReady();
        }
    }
}