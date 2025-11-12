package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.ModConfig;
import btw.community.btwmusicplayer.data.SongConditions;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    private static final List<SongRule> allSongRules = new ArrayList<>();
    private static String currentlyPlayingSongFile = null;
    private static int debugTicks = 0;
    private static long lastCombatEventTick = -1;

    @Shadow private SoundSystem sndSystem;
    @Shadow private GameSettings options;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ResourceManager resourceManager, GameSettings gameSettings, File assetsDir, CallbackInfo ci) {
        File gameDir = FabricLoader.getInstance().getGameDir().toFile();
        ModConfig.getInstance();
        loadSoundPacks(gameDir);
    }

    private void loadSoundPacks(File gameDir) {
        System.out.println("--- [BTW Music Player] Rozpoczynam ładowanie Sound Packów... ---");
        System.out.println("--- [BTW Music Player] DIAGNOSTYKA: Używam katalogu gry: " + gameDir.getAbsolutePath());

        File soundPacksDir = new File(gameDir, "soundpacks");

        ModConfig config = ModConfig.getInstance();
        boolean loadAll = config.loadingMode.equalsIgnoreCase("ALL");

        if (!soundPacksDir.exists() || !soundPacksDir.isDirectory()) {
            System.out.println("--- [BTW Music Player] Folder '" + soundPacksDir.getAbsolutePath() + "' nie istnieje lub nie jest folderem. Ładowanie przerwane.");
            return;
        }

        Gson gson = new Gson();
        Type songRuleListType = new TypeToken<ArrayList<SongRule>>(){}.getType();

        for (File soundPack : soundPacksDir.listFiles()) {
            if (soundPack.isDirectory()) {
                if (!loadAll && !soundPack.getName().equalsIgnoreCase(config.singlePackName)) {
                    System.out.println("--- [BTW Music Player] Pomijam pack: " + soundPack.getName() + " (Single mode: " + config.singlePackName + ")");
                    continue;
                }
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
        ci.cancel();
        if (this.sndSystem == null || this.options.musicVolume == 0.0f) return;

        debugTicks++;
        boolean shouldLog = debugTicks % 100 == 0;

        updateCombatState(shouldLog);

        SongRule bestRule = null;
        for (SongRule rule : allSongRules) {
            if (checkConditions(rule.conditions, shouldLog)) {
                if (bestRule == null || rule.priority > bestRule.priority) {
                    bestRule = rule;
                }
            }
        }

        if (bestRule != null) {
            if (!bestRule.file.equals(currentlyPlayingSongFile)) {
                System.out.println("[Music Player] Zmieniam utwór na: " + bestRule.file + " (Priorytet: " + bestRule.priority + ")");
                this.sndSystem.stop("BgMusic");
                try {
                    File songFile = new File(bestRule.soundPackPath, bestRule.file);
                    if (songFile.exists()) {
                        URL songUrl = songFile.toURI().toURL();
                        this.sndSystem.backgroundMusic("BgMusic", songUrl, bestRule.file, false);
                        this.sndSystem.setVolume("BgMusic", this.options.musicVolume);
                        this.sndSystem.play("BgMusic");
                        currentlyPlayingSongFile = bestRule.file;
                    } else {
                        System.err.println("[Music Player] BŁĄD: Plik nie istnieje: " + songFile.getAbsolutePath());
                        currentlyPlayingSongFile = null;
                    }
                } catch (Exception e) {
                    System.err.println("[Music Player] BŁĄD: Nie można odtworzyć pliku: " + bestRule.file);
                    e.printStackTrace();
                    currentlyPlayingSongFile = null;
                }
            }
        } else {
            if (currentlyPlayingSongFile != null) {
                System.out.println("[Music Player] Brak pasujących reguł. Zatrzymuję muzykę.");
                this.sndSystem.stop("BgMusic");
                currentlyPlayingSongFile = null;
            }
        }
    }

    private void updateCombatState(boolean log) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return;

        boolean isThreatened = false;
        String reason = "None";

        List<Entity> nearbyEntities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(16.0, 8.0, 16.0));

        for (Entity entity : nearbyEntities) {
            if (entity instanceof IMob && entity.isEntityAlive()) {
                isThreatened = true;
                break;
            }
        }

        long worldTime = mc.theWorld.getTotalWorldTime();
        boolean isCurrentlyInCombat = (lastCombatEventTick > 0 && (worldTime - lastCombatEventTick) < 100);

        if (player.hurtTime > 0 && isThreatened) {
            lastCombatEventTick = worldTime;
            reason = "Player Hurt";
        }

        else if (isCurrentlyInCombat && isThreatened) {
            lastCombatEventTick = worldTime;
            reason = "Sustained by Threat";
        }

        if (log) {
            System.out.println("[Music Player Combat Debug] Threatened: " + isThreatened + ". Update Reason: " + reason);
        }
    }

    private boolean checkConditions(SongConditions conditions, boolean log) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return false;

        if (conditions.is_in_combat != null) {
            boolean isInCombat = false;
            if (lastCombatEventTick > 0) {
                long timeSinceLastEvent = mc.theWorld.getTotalWorldTime() - lastCombatEventTick;
                if (timeSinceLastEvent < 100) {
                    isInCombat = true;
                }
            }

            if (log) {
                System.out.println(
                        "[Music Player Debug] W walce: " + isInCombat +
                                " (lastEvent: " + lastCombatEventTick +
                                ", timeSince: " + (mc.theWorld.getTotalWorldTime() - lastCombatEventTick) + ")"
                );
            }

            if (conditions.is_in_combat != isInCombat) return false;
        }

        if (conditions.dimension != null) {
            int dimensionId = player.worldObj.provider.dimensionId;
            String dimensionName = (dimensionId == -1) ? "the_nether" : (dimensionId == 0) ? "overworld" : (dimensionId == 1) ? "the_end" : "unknown";
            if (!conditions.dimension.equalsIgnoreCase(dimensionName)) return false;
        }

        if (conditions.time_of_day != null) {
            long time = player.worldObj.getWorldTime() % 24000;
            String timeName = (time >= 0 && time < 13000) ? "day" : "night";
            if (log) System.out.println("[Music Player Debug] Czas: " + time + " (" + timeName + ")");
            if (!conditions.time_of_day.equalsIgnoreCase(timeName)) return false;
        }

        if (conditions.biome != null) {
            String biomeName = player.worldObj.getBiomeGenForCoords((int)player.posX, (int)player.posZ).biomeName;
            String normalizedBiomeName = biomeName.toLowerCase().replace(' ', '_');
            if (log) System.out.println("[Music Player Debug] Biom: " + biomeName + " (" + normalizedBiomeName + ")");
            if (!conditions.biome.equalsIgnoreCase(normalizedBiomeName)) return false;
        }

        return true;
    }
}