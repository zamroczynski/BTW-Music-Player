package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.GameSettings;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.net.URL;

/**
 * Manages the state of music playback (IDLE, PLAYING, FADING_IN, FADING_OUT).
 * Handles the technical details of playing, stopping, and transitioning between songs.
 * Hardened against SoundSystem failures.
 */
public class PlaybackStateMachine {
    private final GameSettings options;

    private MusicState musicState = MusicState.IDLE;
    private long transitionStartTime = 0;
    private String currentSongPath = null;

    public PlaybackStateMachine(GameSettings options) {
        this.options = options;
    }

    public void update(SongRule targetSongRule, SoundSystem sndSystem, boolean log) {
        if (sndSystem == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        int fadeDurationMs = ModConfig.getInstance().fadeDurationMs;
        float progress = (fadeDurationMs <= 0) ? 1.1f : (float)(currentTime - transitionStartTime) / fadeDurationMs;

        String targetSongPath = (targetSongRule != null) ? targetSongRule.file : null;

        try {
            switch (musicState) {
                case IDLE:
                    if (targetSongRule != null) {
                        playNewSong(targetSongRule, sndSystem, 0.0f);
                        this.transitionStartTime = currentTime;
                        changeState(MusicState.FADING_IN, log);
                    }
                    break;

                case PLAYING:
                    if (targetSongPath == null || !targetSongPath.equals(this.currentSongPath)) {
                        this.transitionStartTime = currentTime;
                        changeState(MusicState.FADING_OUT, log);
                    } else {
                        if (sndSystem.playing("BgMusic")) {
                            sndSystem.setVolume("BgMusic", options.musicVolume);
                        }
                    }
                    break;

                case FADING_IN:
                    if (targetSongPath == null || !targetSongPath.equals(this.currentSongPath)) {
                        this.transitionStartTime = currentTime;
                        changeState(MusicState.FADING_OUT, log);
                    } else if (progress >= 1.0f) {
                        sndSystem.setVolume("BgMusic", options.musicVolume);
                        changeState(MusicState.PLAYING, log);
                    } else {
                        sndSystem.setVolume("BgMusic", progress * options.musicVolume);
                    }
                    break;

                case FADING_OUT:
                    if (progress >= 1.0f) {
                        safeStop(sndSystem, "BgMusic");
                        this.currentSongPath = null;

                        if (targetSongRule != null) {
                            playNewSong(targetSongRule, sndSystem, 0.0f);
                            this.transitionStartTime = currentTime;
                            changeState(MusicState.FADING_IN, log);
                        } else {
                            changeState(MusicState.IDLE, log);
                        }
                    } else {
                        float vol = (1.0f - progress) * options.musicVolume;
                        if (vol < 0.0f) vol = 0.0f;
                        sndSystem.setVolume("BgMusic", vol);
                    }
                    break;
            }
        } catch (Exception e) {
            // CRITICAL RECOVERY
            MusicLogger.error("[Playback] Critical error in update loop: " + e.getMessage());
            e.printStackTrace();
            this.musicState = MusicState.IDLE;
            this.currentSongPath = null;
        }
    }

    public void playNewSong(SongRule rule, SoundSystem sndSystem, float initialVolume) {
        if (sndSystem == null) return;

        try {
            File songFile = new File(rule.musicPackPath, rule.file);
            if (songFile.exists()) {
                this.currentSongPath = rule.file;
                URL songUrl = songFile.toURI().toURL();

                safeStop(sndSystem, "BgMusic");

                sndSystem.backgroundMusic("BgMusic", songUrl, rule.file, false);
                sndSystem.setVolume("BgMusic", initialVolume * options.musicVolume);
                sndSystem.play("BgMusic");

                MusicLogger.log("[Playback] Started playing: " + rule.file);
            } else {
                MusicLogger.error("File does not exist: " + songFile.getAbsolutePath());
                this.currentSongPath = null;
            }
        } catch (Exception e) {
            MusicLogger.error("Cannot open file: " + rule.file + ". Error: " + e.getMessage());
            e.printStackTrace();
            this.currentSongPath = null;
        }
    }

    private void safeStop(SoundSystem sndSystem, String sourceName) {
        try {
            if (sndSystem.playing(sourceName)) {
                sndSystem.stop(sourceName);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void changeState(MusicState newState, boolean log) {
        if (this.musicState != newState) {
            if (log) MusicLogger.log("[Playback] State changed: " + this.musicState + " -> " + newState);
            this.musicState = newState;
        }
    }

    public MusicState getState() {
        return this.musicState;
    }
}