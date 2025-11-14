package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.GameSettings;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.net.URL;

/**
 * Manages the state of music playback (IDLE, PLAYING, FADING_IN, FADING_OUT).
 * Handles the technical details of playing, stopping, and transitioning between songs.
 */
public class PlaybackStateMachine {
    private static final long FADE_DURATION_MS = 1000;

    private final SoundSystem sndSystem;
    private final GameSettings options;

    private MusicState musicState = MusicState.IDLE;
    private long transitionStartTime = 0;
    private String currentSongPath = null; // Store path to check for changes

    public PlaybackStateMachine(SoundSystem sndSystem, GameSettings options) {
        this.sndSystem = sndSystem;
        this.options = options;
    }

    public void update(SongRule targetSongRule, boolean log) {
        long currentTime = System.currentTimeMillis();
        float progress = (float)(currentTime - transitionStartTime) / FADE_DURATION_MS;
        String targetSongPath = (targetSongRule != null) ? targetSongRule.file : null;

        switch (musicState) {
            case IDLE:
                if (targetSongRule != null) {
                    playNewSong(targetSongRule, 0.0f);
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
                    this.sndSystem.setVolume("BgMusic", options.musicVolume);
                    changeState(MusicState.PLAYING, log);
                } else {
                    this.sndSystem.setVolume("BgMusic", progress * options.musicVolume);
                }
                break;

            case FADING_OUT:
                if (progress >= 1.0f) {
                    this.sndSystem.stop("BgMusic");
                    this.currentSongPath = null;
                    if (targetSongRule != null) {
                        playNewSong(targetSongRule, 0.0f);
                        this.transitionStartTime = currentTime;
                        changeState(MusicState.FADING_IN, log);
                    } else {
                        changeState(MusicState.IDLE, log);
                    }
                } else {
                    this.sndSystem.setVolume("BgMusic", (1.0f - progress) * options.musicVolume);
                }
                break;
        }
    }

    public void playNewSong(SongRule rule, float initialVolume) {
        try {
            File songFile = new File(rule.soundPackPath, rule.file);
            if (songFile.exists()) {
                this.currentSongPath = rule.file;
                URL songUrl = songFile.toURI().toURL();
                this.sndSystem.backgroundMusic("BgMusic", songUrl, rule.file, false);
                this.sndSystem.setVolume("BgMusic", initialVolume * options.musicVolume);
                this.sndSystem.play("BgMusic");
                MusicLogger.log("[Playback] Starting to play: " + rule.file);
            } else {
                MusicLogger.error("File does not exist: " + songFile.getAbsolutePath());
            }
        } catch (Exception e) {
            MusicLogger.error("Cannot open file: " + rule.file);
            e.printStackTrace();
        }
    }

    private void changeState(MusicState newState, boolean log) {
        if (this.musicState != newState) {
            if (log) MusicLogger.log("[Playback] State changed: " + this.musicState + " -> " + newState);
            this.musicState = newState;
        }
    }
}