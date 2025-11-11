package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.data.SongRule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    private static final List<SongRule> allSongRules = new ArrayList<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ResourceManager resourceManager, GameSettings gameSettings, File assetsDir, CallbackInfo ci) {
        File gameDir = FabricLoader.getInstance().getGameDir().toFile();

        loadSoundPacks(gameDir);
    }

    private void loadSoundPacks(File gameDir) {
        System.out.println("--- [BTW Music Player] Rozpoczynam ładowanie Sound Packów... ---");
        System.out.println("--- [BTW Music Player] DIAGNOSTYKA: Używam katalogu gry: " + gameDir.getAbsolutePath());

        File soundPacksDir = new File(gameDir, "soundpacks");

        if (!soundPacksDir.exists() || !soundPacksDir.isDirectory()) {
            System.out.println("--- [BTW Music Player] Folder '" + soundPacksDir.getAbsolutePath() + "' nie istnieje lub nie jest folderem. Ładowanie przerwane.");
            return;
        }

        Gson gson = new Gson();
        Type songRuleListType = new TypeToken<ArrayList<SongRule>>(){}.getType();

        for (File soundPack : soundPacksDir.listFiles()) {
            if (soundPack.isDirectory()) {
                File songsJsonFile = new File(soundPack, "songs.json");
                if (songsJsonFile.exists()) {
                    System.out.println("--- [BTW Music Player] Znaleziono sound pack: " + soundPack.getName());
                    try (FileReader reader = new FileReader(songsJsonFile)) {
                        List<SongRule> rules = gson.fromJson(reader, songRuleListType);
                        for (SongRule rule : rules) {
                            rule.soundPackPath = soundPack.getAbsolutePath();
                        }
                        allSongRules.addAll(rules);
                        System.out.println("--- [BTW Music Player] Załadowano " + rules.size() + " reguł.");
                    } catch (Exception e) {
                        System.err.println("--- [BTW Music Player] Błąd podczas ładowania sound packa: " + soundPack.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("--- [BTW Music Player] Zakończono ładowanie. Całkowita liczba reguł: " + allSongRules.size());
    }

    @Inject(method = "playRandomMusicIfReady", at = @At("HEAD"), cancellable = true)
    private void onPlayRandomMusic(CallbackInfo ci) {

        // TODO: Implementacja pętli sprawdzającej warunki

        ci.cancel();
    }
}