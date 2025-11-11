package btw.community.btwmusicplayer.mixin;

import net.minecraft.src.GameSettings;
import net.minecraft.src.ResourceManager;
import net.minecraft.src.SoundManager;
import net.minecraft.src.SoundPool;
import net.minecraft.src.SoundPoolEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.util.List;
import java.util.Map;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Shadow private SoundSystem sndSystem;
    @Shadow private SoundPool soundPoolMusic;
    @Shadow private GameSettings options;

    private boolean hasDumpedMusicList = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ResourceManager par1ResourceManager, GameSettings par2GameSettings, File par3File, CallbackInfo ci) {
        System.out.println("--- [BTW Music Player] Rejestruję dodatkową muzykę... ---");

        SoundManager thisManager = (SoundManager)(Object)this;

        thisManager.addMusic("btw-music-player:test_music.ogg");

        System.out.println("--- [BTW Music Player] Ręczna rejestracja zakończona. ---");
    }

    @Inject(method = "playRandomMusicIfReady", at = @At("HEAD"), cancellable = true)
    private void onPlayRandomMusic(CallbackInfo ci) {

        if (!this.hasDumpedMusicList) {
            System.out.println("--- [BTW Music Player] Dostępne klucze w puli muzyki ---");
            Map<String, List<SoundPoolEntry>> soundMap = ((SoundPoolAccessor) this.soundPoolMusic).getSoundMap();
            for (String soundKey : soundMap.keySet()) {
                System.out.println("Znaleziono klucz: " + soundKey);
            }
            System.out.println("---------------------------------------------------------");
            this.hasDumpedMusicList = true;
        }

        if (this.sndSystem == null || this.options.musicVolume == 0.0f || this.sndSystem.playing("BgMusic")) {
            ci.cancel();
            return;
        }

        String soundName = "btw-music-player:test_music";

        SoundPoolEntry musicToPlay = this.soundPoolMusic.getRandomSoundFromSoundPool(soundName);

        if (musicToPlay != null) {
            System.out.println("BTW Music Player: Odtwarzam testowy utwór: " + soundName);

            this.sndSystem.backgroundMusic("BgMusic", musicToPlay.getSoundUrl(), musicToPlay.getSoundName(), false);
            this.sndSystem.play("BgMusic");

        }

        ci.cancel();
    }
}