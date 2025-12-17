package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.*;
import btw.community.btwmusicplayer.MusicCombatTracker;
import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Shadow private SoundSystem sndSystem;
    @Shadow private GameSettings options;
    @Shadow private boolean loaded;

    private static final int DEBUG_LOG_INTERVAL_TICKS = 100;
    private static int tickCounter = 0;
    private String lastScreenName = "";
    private static boolean hasLoggedInitFailure = false;

    private MusicCombatTracker combatTracker;
    private ConditionEvaluator conditionEvaluator;
    private PlaylistManager playlistManager;
    private PlaybackStateMachine playbackStateMachine;

    private int lastDimensionId = Integer.MIN_VALUE;
    private boolean wasJukeboxPlaying = false;

    private OverlayManager overlayManager;
    private OverlayStateMachine overlayStateMachine;

    private void initializeComponents() {
        if (this.combatTracker == null) {
            this.combatTracker = new MusicCombatTracker();
            this.conditionEvaluator = new ConditionEvaluator();
            this.playlistManager = new PlaylistManager();
            this.playbackStateMachine = new PlaybackStateMachine(this.options);
            this.overlayManager = new OverlayManager();
            this.overlayStateMachine = new OverlayStateMachine(this.options);
            btwmusicplayerAddon.getMusicContext().registerComponents(this.combatTracker, this.playlistManager, this.conditionEvaluator, this.overlayManager);
            MusicLogger.always("[SoundManager] Music Player components initialized and registered.");
        }
    }

    @Inject(method = "playRandomMusicIfReady", at = @At("HEAD"), cancellable = true)
    private void onPlayRandomMusic(CallbackInfo ci) {
        ci.cancel();

        // 1. Safety Checks
        if (!this.loaded || this.sndSystem == null || this.options == null) return;
        if (this.options.musicVolume == 0.0f) return;

        // 2. Initialize
        initializeComponents();
        tickCounter++;
        boolean shouldLog = tickCounter % DEBUG_LOG_INTERVAL_TICKS == 0;
        Minecraft mc = Minecraft.getMinecraft();

        // 3. Jukebox Logic (Override)
        boolean isJukeboxPlaying = this.sndSystem.playing("streaming");
        if (isJukeboxPlaying) {
            if (!wasJukeboxPlaying) {
                MusicLogger.always("[SoundManager] Jukebox detected! Suppressing BTW Music Player.");
                wasJukeboxPlaying = true;
            }
            if (playbackStateMachine != null) playbackStateMachine.update(null, this.sndSystem, shouldLog);
            if (overlayStateMachine != null) overlayStateMachine.update(null, this.sndSystem, shouldLog); // Wyciszamy też overlay
            return;
        } else {
            if (wasJukeboxPlaying) {
                MusicLogger.always("[SoundManager] Jukebox stopped. Resuming logic.");
                wasJukeboxPlaying = false;
                if (playlistManager != null) playlistManager.forceReset();
            }
        }

        // 4. Update Global Trackers
        combatTracker.update(mc, btwmusicplayerAddon.getMusicContext());

        if (shouldLog) {
            MusicLogger.trace("[SoundManager] --- Update Tick ---");
            if (overlayManager == null) MusicLogger.error("CRITICAL: OverlayManager is null!");
            if (playbackStateMachine == null) MusicLogger.error("CRITICAL: PlaybackStateMachine is null!");
        }

        // 5. UPDATE BACKGROUND MUSIC LOGIC
        playlistManager.update(conditionEvaluator, combatTracker, mc, shouldLog);

        if (playbackStateMachine != null
                && playbackStateMachine.getState() == MusicState.PLAYING
                && !this.sndSystem.playing("BgMusic")) {
            if (playlistManager.hasPendingChange()) {
                playlistManager.forceCommitPendingChange();
            } else {
                if (shouldLog) MusicLogger.log("[SoundManager] Background song finished. Advancing.");
                playlistManager.advanceToNextSong();
            }
        }

        SongRule targetSong = playlistManager.getCurrentSongRule();
        if (playbackStateMachine != null) {
            playbackStateMachine.update(targetSong, this.sndSystem, shouldLog);
        }

        // 6. UPDATE OVERLAY LOGIC (Heartbeat)
        if (overlayManager != null) {
            overlayManager.update(conditionEvaluator, combatTracker, mc, shouldLog);
            SongRule targetOverlay = overlayManager.getCurrentOverlayRule();

            if (shouldLog && targetOverlay != null) {
                MusicLogger.trace("[SoundManager] Target Overlay: " + targetOverlay.file);
            }

            if (overlayStateMachine != null) {
                overlayStateMachine.update(targetOverlay, this.sndSystem, shouldLog);
            }
        }
    }
}