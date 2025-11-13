package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.ModConfig;
import btw.community.btwmusicplayer.MusicPlayerState;
import btw.community.btwmusicplayer.MusicState;
import btw.community.btwmusicplayer.data.SongConditions;
import btw.community.btwmusicplayer.data.SongRule;
import btw.entity.mob.BTWSquidEntity;
import btw.entity.mob.DireWolfEntity;
import net.minecraft.src.EntityDragon;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    private static MusicState musicState = MusicState.IDLE;
    private static final List<SongRule> allSongRules = new ArrayList<>();

    private static List<SongRule> currentPlaylist = new ArrayList<>();
    private static int currentPlaylistIndex = 0;

    private static String currentSongFile = null;

    private static long transitionStartTime = 0;
    private static final long FADE_DURATION_MS = 1000;

    private static int debugTicks = 0;
    private static long lastCombatEventTick = -1;

    private static long victoryCooldownEndTick = -1;
    private static boolean hasLoggedCooldownEnd = true;

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
        if (this.sndSystem == null || this.options == null || this.options.musicVolume == 0.0f) return;

        debugTicks++;
        boolean shouldLog = debugTicks % 100 == 0;
//        boolean shouldLog = true;
        updateCombatState(shouldLog);

        List<SongRule> bestPlaylist = determineBestPlaylist(shouldLog);

        String targetSongFile = null;
        if (!bestPlaylist.isEmpty()) {
            if (!arePlaylistsEqual(bestPlaylist, currentPlaylist)) {
                if (shouldLog) System.out.println("[Music Player Playlist] Zmiana kontekstu, tworzę nową playlistę.");
                currentPlaylist = bestPlaylist;
                Collections.shuffle(currentPlaylist);
                currentPlaylistIndex = 0;
            }
            targetSongFile = currentPlaylist.get(currentPlaylistIndex).file;
        } else {
            currentPlaylist.clear();
            currentPlaylistIndex = 0;
        }

        long currentTime = System.currentTimeMillis();
        float progress = (float)(currentTime - transitionStartTime) / FADE_DURATION_MS;

        switch (musicState) {
            case IDLE:
                if (targetSongFile != null) {
                    currentSongFile = targetSongFile;
                    playNewSong(currentPlaylist.get(currentPlaylistIndex), 0.0f);
                    transitionStartTime = currentTime;
                    changeState(MusicState.FADING_IN, shouldLog);
                }
                break;

            case PLAYING:
                if (!this.sndSystem.playing("BgMusic")) {
                    if (shouldLog) System.out.println("[Music Player Playlist Debug] Utwór '" + currentSongFile + "' zakończył się.");
                    currentPlaylistIndex = (currentPlaylistIndex + 1) % currentPlaylist.size();
                    String nextSong = currentPlaylist.get(currentPlaylistIndex).file;
                    if (shouldLog) System.out.println("[Music Player Playlist Debug] Przechodzę do następnego: '" + nextSong + "'");

                    currentSongFile = nextSong;
                    playNewSong(currentPlaylist.get(currentPlaylistIndex), this.options.musicVolume);
                    break;
                }

                if (targetSongFile == null) {
                    transitionStartTime = currentTime;
                    changeState(MusicState.FADING_OUT, shouldLog);
                } else if (!targetSongFile.equals(currentSongFile)) {
                    transitionStartTime = currentTime;
                    changeState(MusicState.FADING_OUT, shouldLog);
                }
                break;

            case FADING_IN:
                if (progress >= 1.0f) {
                    this.sndSystem.setVolume("BgMusic", options.musicVolume);
                    changeState(MusicState.PLAYING, shouldLog);
                } else {
                    this.sndSystem.setVolume("BgMusic", progress * options.musicVolume);
                    if (shouldLog) System.out.println("[Music Player Fade Debug] Fading In: " + currentSongFile + " (" + (int)(progress * 100) + "%)");
                }
                if (targetSongFile != null && !targetSongFile.equals(currentSongFile)) {
                    transitionStartTime = currentTime;
                    changeState(MusicState.FADING_OUT, shouldLog);
                }
                break;

            case FADING_OUT:
                if (progress >= 1.0f) {
                    this.sndSystem.stop("BgMusic");
                    currentSongFile = targetSongFile;

                    if (currentSongFile != null) {
                        playNewSong(currentPlaylist.get(currentPlaylistIndex), 0.0f);
                        transitionStartTime = currentTime;
                        changeState(MusicState.FADING_IN, shouldLog);
                    } else {
                        changeState(MusicState.IDLE, shouldLog);
                    }
                } else {
                    this.sndSystem.setVolume("BgMusic", (1.0f - progress) * options.musicVolume);
                    if (shouldLog) System.out.println("[Music Player Fade Debug] Fading Out... (" + (int)(progress * 100) + "%)");
                }
                break;
        }
    }

    private List<SongRule> determineBestPlaylist(boolean log) {
        List<SongRule> potentialRules = new ArrayList<>();
        for (SongRule rule : allSongRules) {
            if (checkConditions(rule.conditions, log)) {
                potentialRules.add(rule);
            }
        }

        List<SongRule> bestPlaylist = new ArrayList<>();
        int bestPriority = -1;
        if (!potentialRules.isEmpty()) {
            for (SongRule rule : potentialRules) {
                if (rule.priority > bestPriority) {
                    bestPriority = rule.priority;
                }
            }
            for (SongRule rule : potentialRules) {
                if (rule.priority == bestPriority) {
                    bestPlaylist.add(rule);
                }
            }
        }
        return bestPlaylist;
    }

    private boolean arePlaylistsEqual(List<SongRule> list1, List<SongRule> list2) {
        if (list1.size() != list2.size()) return false;
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    private void changeState(MusicState newState, boolean log) {
        if (musicState != newState) {
            if (log) System.out.println("[Music Player State Debug] Zmiana stanu: " + musicState + " -> " + newState);
            musicState = newState;
        }
    }

    private void playNewSong(SongRule rule, float initialVolume) {
        try {
            File songFile = new File(rule.soundPackPath, rule.file);
            if (songFile.exists()) {
                URL songUrl = songFile.toURI().toURL();
                this.sndSystem.backgroundMusic("BgMusic", songUrl, rule.file, false);
                this.sndSystem.setVolume("BgMusic", initialVolume * options.musicVolume);
                this.sndSystem.play("BgMusic");
            } else {
                System.err.println("[Music Player] BŁĄD: Plik nie istnieje: " + songFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[Music Player] BŁĄD: Nie można odtworzyć pliku: " + rule.file);
            e.printStackTrace();
        }
    }

    private void updateCombatState(boolean log) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return;

        long worldTime = mc.theWorld.getTotalWorldTime();


        if (MusicPlayerState.consumeVictorySignalIfPresent()) {
            System.out.println("[MusicPlayer LOG @ " + worldTime + "] Otrzymano sygnał zwycięstwa nad bossem.");
            victoryCooldownEndTick = worldTime + 500;
            lastCombatEventTick = -1;

            MusicPlayerState.wasAttackJustTriggered();

            hasLoggedCooldownEnd = false;
            System.out.println("[MusicPlayer LOG @ " + worldTime + "] Stan walki zresetowany. Flaga ataku wyczyszczona. Cooldown zwycięstwa aktywny do ticku: " + victoryCooldownEndTick);
            return;
        }

        if (worldTime <= victoryCooldownEndTick) {
            return;
        }

        if (!hasLoggedCooldownEnd) {
            System.out.println("[MusicPlayer LOG @ " + worldTime + "] Cooldown zwycięstwa zakończony. Wznawiam normalne sprawdzanie stanu walki.");
            hasLoggedCooldownEnd = true;
        }

        String reason = "None";
        boolean combatEventDetected = false;

        if (worldTime > victoryCooldownEndTick) {
            List<Entity> nearbyEntities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(64.0, 64.0, 64.0));
            boolean isBossPresent = false;
            for (Entity entity : nearbyEntities) {
                if (entity.isEntityAlive() && entity instanceof IBossDisplayData) { // Sprawdzamy ogólny interfejs bossa
                    isBossPresent = true;
                    break;
                }
            }

            if (isBossPresent) {
                if (log)
                    System.out.println("[Music Player Combat LOG] -> Priorytet 1: Wykryto Bossa. Podtrzymuję walkę.");
                combatEventDetected = true;
                reason = "Sustained by Boss Presence";
            } else {
                if (log)
                    System.out.println("[Music Player Combat LOG] -> Priorytet 1: Brak Bossa. Sprawdzam inne zagrożenia...");

                boolean triggerFired = false;
                boolean isThreatened = false;

                List<Entity> nearbyThreats = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(16.0, 8.0, 16.0));
                for (Entity entity : nearbyThreats) {
                    if (entity.isEntityAlive() && (entity instanceof IMob || entity instanceof BTWSquidEntity || (entity instanceof EntityWolf && ((EntityWolf) entity).isAngry()))) {
                        isThreatened = true;
                        break;
                    }
                }

                if (player.hurtTime > 0 && isThreatened) {
                    if (log) System.out.println("[Music Player Combat LOG] -> Zapalnik: Player Hurt (TRUE)");
                    triggerFired = true;
                    reason = "Player Hurt by Nearby Threat";
                }
                if (!triggerFired && MusicPlayerState.wasAttackJustTriggered()) {
                    if (log) System.out.println("[Music Player Combat LOG] -> Zapalnik: Player Attacked (TRUE)");
                    triggerFired = true;
                    reason = "Player Initiated Attack";
                }
                if (!triggerFired && player.riddenByEntity instanceof BTWSquidEntity && player.riddenByEntity.isEntityAlive()) {
                    if (log) System.out.println("[Music Player Combat LOG] -> Zapalnik: Squid Headcrab (TRUE)");
                    triggerFired = true;
                    reason = "Squid Headcrab Started";
                }

                boolean sustainFired = false;
                boolean isCurrentlyInCombat = (lastCombatEventTick > 0 && (worldTime - lastCombatEventTick) < 100);

                if (isCurrentlyInCombat) {
                    if (isThreatened) {
                        if (log) System.out.println("[Music Player Combat LOG] -> Podtrzymywacz: isThreatened (TRUE)");
                        sustainFired = true;
                        reason = "Sustained by Threat Presence";
                    }
                }

                if (triggerFired || sustainFired) {
                    combatEventDetected = true;
                }
            }

            if (combatEventDetected) {
                if (lastCombatEventTick < worldTime - 5)
                    System.out.println("[MusicPlayer LOG @ " + worldTime + "] ZDARZENIE BOJOWE WYKRYTE. Powód: " + reason);
                lastCombatEventTick = worldTime;
            }
        }
    }

    private boolean checkConditions(SongConditions conditions, boolean log) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return false;
        long worldTime = mc.theWorld.getTotalWorldTime();

        if (conditions.victory_after_boss != null) {
            boolean victoryActive = worldTime <= victoryCooldownEndTick;
            if (log) System.out.println("[LOG @ " + worldTime + "] checkConditions: Sprawdzam 'victory_after_boss'. Aktywny: " + victoryActive);
            return victoryActive;
        }

        if (worldTime <= victoryCooldownEndTick && (conditions.is_in_combat != null || conditions.boss_type != null)) {
            return false;
        }


        if (conditions.boss_type != null) {
            boolean isFightingBoss = false;
            String detectedBoss = "none";

            List<Entity> nearbyEntities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(64.0, 64.0, 64.0));
            for (Entity entity : nearbyEntities) {
                if (entity.isEntityAlive()) {
                    if (conditions.boss_type.equalsIgnoreCase("wither") && entity instanceof EntityWither) {
                        isFightingBoss = true;
                        detectedBoss = "wither";
                        break;
                    }
                    else if (conditions.boss_type.equalsIgnoreCase("ender_dragon") && entity instanceof EntityDragon) {
                        isFightingBoss = true;
                        detectedBoss = "ender_dragon";
                        break;
                    }
                }
            }

            if (log) {
                System.out.println("[Music Player Debug] Walka z bossem (" + conditions.boss_type + "): " + isFightingBoss + ". Wykryto: " + detectedBoss);
            }

            if (!isFightingBoss) {
                return false;
            }
        }

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
                        "[Music Player State Debug] W walce: " + isInCombat +
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

        if (conditions.is_in_cave != null) {
            boolean isBelowSeaLevel = player.posY < 60;
            boolean canSeeSky = player.worldObj.canBlockSeeTheSky((int)player.posX, (int)player.posY, (int)player.posZ);
            boolean isCave = isBelowSeaLevel && !canSeeSky;

            if (log) {
//                System.out.println("[Music Player Debug] Jaskinia: " + isCave + " (Y: " + (int)player.posY + ", CanSeeSky: " + canSeeSky + ")");
            }

            if (conditions.is_in_cave != isCave) {
                return false;
            }
        }

        if (conditions.weather != null) {
            String currentWeather;
            if (player.worldObj.isThundering()) {
                currentWeather = "storm";
            } else {
                currentWeather = "clear";
            }

            if (log) {
//                System.out.println("[Music Player Debug] Pogoda: " + currentWeather);
            }

            if (!conditions.weather.equalsIgnoreCase(currentWeather)) {
                return false;
            }
        }

        if (conditions.time_of_day != null) {
            long time = player.worldObj.getWorldTime() % 24000;
            String timeName = (time >= 0 && time < 13000) ? "day" : "night";
//            if (log) System.out.println("[Music Player Debug] Czas: " + time + " (" + timeName + ")");
            if (!conditions.time_of_day.equalsIgnoreCase(timeName)) return false;
        }

        if (conditions.biome != null) {
            String biomeName = player.worldObj.getBiomeGenForCoords((int)player.posX, (int)player.posZ).biomeName;
            String normalizedBiomeName = biomeName.toLowerCase().replace(' ', '_');
//            if (log) System.out.println("[Music Player Debug] Biom: " + biomeName + " (" + normalizedBiomeName + ")");
            if (!conditions.biome.equalsIgnoreCase(normalizedBiomeName)) return false;
        }

        return true;
    }
}