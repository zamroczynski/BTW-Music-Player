package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.GameSettings;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.net.URL;

/**
 * Manages the state of music playback (IDLE, PLAYING, FADING_IN, FADING_OUT).
 * Handles the technical details of playing, stopping, and transitioning between songs.
 * Refactored to accept SoundSystem dynamically and use configurable fade duration.
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
            if (log) MusicLogger.trace("[Playback] SoundSystem is null, skipping update.");
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
                        sndSystem.stop("BgMusic");
                        this.currentSongPath = null;
                        if (targetSongRule != null) {
                            playNewSong(targetSongRule, sndSystem, 0.0f);
                            this.transitionStartTime = currentTime;
                            changeState(MusicState.FADING_IN, log);
                        } else {
                            changeState(MusicState.IDLE, log);
                        }
                    } else {
                        sndSystem.setVolume("BgMusic", (1.0f - progress) * options.musicVolume);
                    }
                    break;
            }
        } catch (Exception e) {
            MusicLogger.error("[Playback] Critical error in update loop: " + e.getMessage());
            e.printStackTrace();
            this.musicState = MusicState.IDLE;
            this.currentSongPath = null;
        }
    }

    public void playNewSong(SongRule rule, SoundSystem sndSystem, float initialVolume) {
        if (sndSystem == null) {
            MusicLogger.error("[Playback] Cannot play song: SoundSystem is null.");
            return;
        }

        try {
            File songFile = new File(rule.musicPackPath, rule.file);
            if (songFile.exists()) {
                this.currentSongPath = rule.file;
                URL songUrl = songFile.toURI().toURL();

                if (sndSystem.playing("BgMusic")) {
                    sndSystem.stop("BgMusic");
                }

                sndSystem.backgroundMusic("BgMusic", songUrl, rule.file, false);
                sndSystem.setVolume("BgMusic", initialVolume * options.musicVolume);
                sndSystem.play("BgMusic");

                MusicLogger.log("[Playback] Started playing: " + rule.file);
            } else {
                MusicLogger.error("File does not exist: " + songFile.getAbsolutePath());
            }
        } catch (Exception e) {
            MusicLogger.error("Cannot open file: " + rule.file + ". Error: " + e.getMessage());
            e.printStackTrace();
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