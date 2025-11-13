package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.*;
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
                if (shouldLog) MusicLogger.log("[Music Player Playlist] Change of context. Creating a new playlist.");
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
                    if (shouldLog) MusicLogger.log("[Music Player Playlist Debug] Song '" + currentSongFile + "' ended.");
                    currentPlaylistIndex = (currentPlaylistIndex + 1) % currentPlaylist.size();
                    String nextSong = currentPlaylist.get(currentPlaylistIndex).file;
                    if (shouldLog) MusicLogger.log("[Music Player Playlist Debug] : Moving on to the next one'" + nextSong + "'");

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
                    if (shouldLog) MusicLogger.log("[Music Player Fade Debug] Fading In: " + currentSongFile + " (" + (int)(progress * 100) + "%)");
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
                    if (shouldLog) MusicLogger.log("[Music Player Fade Debug] Fading Out... (" + (int)(progress * 100) + "%)");
                }
                break;
        }
    }

    private List<SongRule> determineBestPlaylist(boolean log) {
        List<SongRule> potentialRules = new ArrayList<>();
        for (SongRule rule : MusicManager.getSongRules()) {
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
            if (log) MusicLogger.log("[Music Player State Debug] Change of status: " + musicState + " -> " + newState);
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
                MusicLogger.error("[Music Player] ERROR: File does not exist: " + songFile.getAbsolutePath());
            }
        } catch (Exception e) {
            MusicLogger.error("[Music Player] ERROR: File cannot be open: " + rule.file);
            e.printStackTrace();
        }
    }

    private void updateCombatState(boolean log) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || player.worldObj == null) return;

        long worldTime = mc.theWorld.getTotalWorldTime();


        if (MusicPlayerState.consumeVictorySignalIfPresent()) {
            MusicLogger.log("[MusicPlayer LOG @ " + worldTime + "] Signal received: boss defeated.");
            victoryCooldownEndTick = worldTime + 500;
            lastCombatEventTick = -1;

            MusicPlayerState.wasAttackJustTriggered();

            hasLoggedCooldownEnd = false;
            MusicLogger.log("[MusicPlayer LOG @ " + worldTime + "] Battle status reset. Attack flag cleared. Victory cooldown active until tick: " + victoryCooldownEndTick);
            return;
        }

        if (worldTime <= victoryCooldownEndTick) {
            return;
        }

        if (!hasLoggedCooldownEnd) {
            MusicLogger.log("[MusicPlayer LOG @ " + worldTime + "] Victory cooldown complete. Resuming normal combat status checks.");
            hasLoggedCooldownEnd = true;
        }

        String reason = "None";
        boolean combatEventDetected = false;

        if (worldTime > victoryCooldownEndTick) {
            List<Entity> nearbyEntities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, player.boundingBox.expand(64.0, 64.0, 64.0));
            boolean isBossPresent = false;
            for (Entity entity : nearbyEntities) {
                if (entity.isEntityAlive() && entity instanceof IBossDisplayData) {
                    isBossPresent = true;
                    break;
                }
            }

            if (isBossPresent) {
                if (log)
                    MusicLogger.log("[Music Player Combat LOG] -> Priority 1: Boss detected. Continue the fight.");
                combatEventDetected = true;
                reason = "Sustained by Boss Presence";
            } else {
                if (log)
                    MusicLogger.log("[Music Player Combat LOG] -> Priority 1: No Boss. Checking other threats...");

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
                    if (log) MusicLogger.log("[Music Player Combat LOG] -> Igniter: Player Hurt (TRUE)");
                    triggerFired = true;
                    reason = "Player Hurt by Nearby Threat";
                }
                if (!triggerFired && MusicPlayerState.wasAttackJustTriggered()) {
                    if (log) MusicLogger.log("[Music Player Combat LOG] -> Igniter: Player Attacked (TRUE)");
                    triggerFired = true;
                    reason = "Player Initiated Attack";
                }
                if (!triggerFired && player.riddenByEntity instanceof BTWSquidEntity && player.riddenByEntity.isEntityAlive()) {
                    if (log) MusicLogger.log("[Music Player Combat LOG] -> Igniter: Squid Headcrab (TRUE)");
                    triggerFired = true;
                    reason = "Squid Headcrab Started";
                }

                boolean sustainFired = false;
                boolean isCurrentlyInCombat = (lastCombatEventTick > 0 && (worldTime - lastCombatEventTick) < 100);

                if (isCurrentlyInCombat) {
                    if (isThreatened) {
                        if (log) MusicLogger.log("[Music Player Combat LOG] -> Support: isThreatened (TRUE)");
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
                    MusicLogger.log("[MusicPlayer LOG @ " + worldTime + "] COMBAT EVENT DETECTED. Reason: " + reason);
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
            if (log) MusicLogger.log("[LOG @ " + worldTime + "] checkConditions: 'victory_after_boss'. Active: " + victoryActive);
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
                MusicLogger.log("[Music Player Debug] Boss fight: (" + conditions.boss_type + "): " + isFightingBoss + ". Detected: " + detectedBoss);
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
                MusicLogger.log(
                        "[Music Player State Debug] Combat: " + isInCombat +
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
                MusicLogger.log("[Music Player Debug] Cave: " + isCave + " (Y: " + (int)player.posY + ", CanSeeSky: " + canSeeSky + ")");
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
                MusicLogger.log("[Music Player Debug] Weather: " + currentWeather);
            }

            if (!conditions.weather.equalsIgnoreCase(currentWeather)) {
                return false;
            }
        }

        if (conditions.time_of_day != null) {
            long time = player.worldObj.getWorldTime() % 24000;
            String timeName = (time >= 0 && time < 13000) ? "day" : "night";
            if (log) MusicLogger.log("[Music Player Debug] Time: " + time + " (" + timeName + ")");
            if (!conditions.time_of_day.equalsIgnoreCase(timeName)) return false;
        }

        if (conditions.biome != null) {
            String biomeName = player.worldObj.getBiomeGenForCoords((int)player.posX, (int)player.posZ).biomeName;
            String normalizedBiomeName = biomeName.toLowerCase().replace(' ', '_');
            if (log) MusicLogger.log("[Music Player Debug] Biom: " + biomeName + " (" + normalizedBiomeName + ")");
            if (!conditions.biome.equalsIgnoreCase(normalizedBiomeName)) return false;
        }

        return true;
    }
}