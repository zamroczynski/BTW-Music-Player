package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.GameSettings;
import paulscode.sound.SoundSystem;
import java.io.File;
import java.net.URL;

/**
 * Handles playback for the Overlay channel (e.g. Heartbeat).
 * Operates on a dedicated source name "BtwMusicOverlay".
 */
public class OverlayStateMachine {
    private static final String OVERLAY_SOURCE_NAME = "BtwMusicOverlay";

    private final GameSettings options;
    private MusicState musicState = MusicState.IDLE;
    private long transitionStartTime = 0;
    private String currentSongPath = null;

    public OverlayStateMachine(GameSettings options) {
        this.options = options;
    }

    public void update(SongRule targetRule, SoundSystem sndSystem, boolean log) {
        if (sndSystem == null) return;

        long currentTime = System.currentTimeMillis();
        int fadeDurationMs = ModConfig.getInstance().fadeDurationMs;
        float progress = (fadeDurationMs <= 0) ? 1.1f : (float)(currentTime - transitionStartTime) / fadeDurationMs;
        String targetPath = (targetRule != null) ? targetRule.file : null;

        try {
            switch (musicState) {
                case IDLE:
                    if (targetRule != null) {
                        playOverlay(targetRule, sndSystem);
                        this.transitionStartTime = currentTime;
                        changeState(MusicState.FADING_IN, log);
                    }
                    break;

                case PLAYING:
                    if (targetPath == null || !targetPath.equals(this.currentSongPath)) {
                        this.transitionStartTime = currentTime;
                        changeState(MusicState.FADING_OUT, log);
                    } else {
                        ensureVolume(sndSystem, options.musicVolume);
                    }
                    break;

                case FADING_IN:
                    if (targetPath == null || !targetPath.equals(this.currentSongPath)) {
                        this.transitionStartTime = currentTime;
                        changeState(MusicState.FADING_OUT, log);
                    } else if (progress >= 1.0f) {
                        ensureVolume(sndSystem, options.musicVolume);
                        changeState(MusicState.PLAYING, log);
                    } else {
                        ensureVolume(sndSystem, progress * options.musicVolume);
                    }
                    break;

                case FADING_OUT:
                    if (progress >= 1.0f) {
                        safeStop(sndSystem);
                        this.currentSongPath = null;
                        if (targetRule != null) {
                            playOverlay(targetRule, sndSystem);
                            this.transitionStartTime = currentTime;
                            changeState(MusicState.FADING_IN, log);
                        } else {
                            changeState(MusicState.IDLE, log);
                        }
                    } else {
                        float vol = (1.0f - progress) * options.musicVolume;
                        if (vol < 0.0f) vol = 0.0f;
                        ensureVolume(sndSystem, vol);
                    }
                    break;
            }
        } catch (Exception e) {
            MusicLogger.error("[OverlayStateMachine] Critical error: " + e.getMessage());
            e.printStackTrace();
            this.musicState = MusicState.IDLE;
            this.currentSongPath = null;
        }
    }

    private void playOverlay(SongRule rule, SoundSystem sndSystem) {
        try {
            File songFile = new File(rule.musicPackPath, rule.file);
            if (songFile.exists()) {
                this.currentSongPath = rule.file;
                URL songUrl = songFile.toURI().toURL();

                safeStop(sndSystem);

                MusicLogger.log("[OverlayStateMachine] Attempting to spawn overlay source for: " + rule.file);

                sndSystem.newStreamingSource(true, OVERLAY_SOURCE_NAME, songUrl, rule.file, true, 0, 0, 0, 0, 0);

                if (sndSystem.playing(OVERLAY_SOURCE_NAME)) {
                    MusicLogger.error("[OverlayStateMachine] Source already playing before start command?");
                }

                sndSystem.setLooping(OVERLAY_SOURCE_NAME, true);
                sndSystem.setVolume(OVERLAY_SOURCE_NAME, 0.0f);
                sndSystem.play(OVERLAY_SOURCE_NAME);

                if (sndSystem.playing(OVERLAY_SOURCE_NAME)) {
                    MusicLogger.log("[OverlayStateMachine] SUCCESS. Overlay playing.");
                } else {
                    MusicLogger.error("[OverlayStateMachine] FAILURE. Overlay start command sent but not playing. Out of channels?");
                }
            } else {
                MusicLogger.error("[OverlayStateMachine] File missing: " + rule.file);
                this.currentSongPath = null;
            }
        } catch (Exception e) {
            MusicLogger.error("[OverlayStateMachine] Failed to play: " + e.getMessage());
            this.currentSongPath = null;
        }
    }

    private void safeStop(SoundSystem sndSystem) {
        if (sndSystem.playing(OVERLAY_SOURCE_NAME)) {
            sndSystem.stop(OVERLAY_SOURCE_NAME);
            sndSystem.removeSource(OVERLAY_SOURCE_NAME);
        }
    }

    private void ensureVolume(SoundSystem sndSystem, float volume) {
        if (sndSystem.playing(OVERLAY_SOURCE_NAME)) {
            sndSystem.setVolume(OVERLAY_SOURCE_NAME, volume);
        }
    }

    private void changeState(MusicState newState, boolean log) {
        if (this.musicState != newState) {
            if (log) MusicLogger.log("[OverlayStateMachine] State: " + this.musicState + " -> " + newState);
            this.musicState = newState;
        }
    }

    public String getStatusInfo() {
        return this.musicState.toString() + " (" + (currentSongPath != null ? currentSongPath : "None") + ")";
    }
}