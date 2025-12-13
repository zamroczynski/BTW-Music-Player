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

    private void initializeComponents() {
        if (this.combatTracker == null) {
            this.combatTracker = new MusicCombatTracker();
            this.conditionEvaluator = new ConditionEvaluator();
            this.playlistManager = new PlaylistManager();
            this.playbackStateMachine = new PlaybackStateMachine(this.options);
            MusicLogger.always("[SoundManager] Music Player components initialized.");
        }
    }

    @Inject(method = "playRandomMusicIfReady", at = @At("HEAD"), cancellable = true)
    private void onPlayRandomMusic(CallbackInfo ci) {
        ci.cancel();

        if (!this.loaded) {
            if (!hasLoggedInitFailure) {
                MusicLogger.always("[SoundManager] Skipping: SoundManager is NOT LOADED yet.");
                hasLoggedInitFailure = true;
            }
            return;
        }

        if (this.sndSystem == null) {
            if (!hasLoggedInitFailure) {
                MusicLogger.always("[SoundManager] Skipping: SoundSystem is NULL.");
                hasLoggedInitFailure = true;
            }
            return;
        }

        if (this.options == null) {
            return;
        }

        if (this.options.musicVolume == 0.0f) {
            if (tickCounter % 200 == 0) {
                 MusicLogger.trace("[SoundManager] Skipping: Music Volume is 0.");
            }
            return;
        }

        if (hasLoggedInitFailure) {
            MusicLogger.always("[SoundManager] System came online!");
            hasLoggedInitFailure = false;
        }

        initializeComponents();

        tickCounter++;
        boolean shouldLog = tickCounter % DEBUG_LOG_INTERVAL_TICKS == 0;

        Minecraft mc = Minecraft.getMinecraft();

        // 3. Screen Detection
        String currentScreenName = "null";
        try {
            if (mc.currentScreen != null) {
                currentScreenName = mc.currentScreen.getClass().getSimpleName();
            } else {
                currentScreenName = (mc.thePlayer != null) ? "Ingame" : "Null(Startup/Transition)";
            }
        } catch (Exception e) {
            currentScreenName = "ErrorGettingScreen";
        }

        if (!currentScreenName.equals(lastScreenName)) {
            MusicLogger.always("[SoundManager] Screen transition: " + lastScreenName + " -> " + currentScreenName);
            lastScreenName = currentScreenName;
            shouldLog = true;

            if (mc.currentScreen instanceof GuiMainMenu) {
                MusicLogger.always("[SoundManager] Detected GuiMainMenu instance.");
            }
        }

        if (shouldLog) {
            MusicLogger.log("[SoundManager] Heartbeat. Screen: " + currentScreenName +
                    ", Loaded: " + this.loaded +
                    ", State: " + (playbackStateMachine != null ? playbackStateMachine.getState() : "N/A"));
        }

        // 4. Update game state trackers
        combatTracker.update(mc, btwmusicplayerAddon.getMusicContext());

        // 5. Determine the correct playlist
        playlistManager.update(conditionEvaluator, combatTracker, mc, shouldLog);

        // 6. Handle song playback logic
        if (playbackStateMachine != null
                && playbackStateMachine.getState() == MusicState.PLAYING
                && !this.sndSystem.playing("BgMusic")) {

            if (playlistManager.hasPendingChange()) {
                playlistManager.forceCommitPendingChange();
            } else {
                if (shouldLog) MusicLogger.log("[SoundManager] Song stopped (finished or system reset). Advancing.");
                playlistManager.advanceToNextSong();
            }
        }

        SongRule targetSong = playlistManager.getCurrentSongRule();

        if (shouldLog && targetSong != null && playbackStateMachine.getState() == MusicState.IDLE) {
            MusicLogger.log("[SoundManager] Target song exists (" + targetSong.file + ") but State is IDLE. Attempting to play...");
        }

        if (playbackStateMachine != null) {
            playbackStateMachine.update(targetSong, this.sndSystem, shouldLog);
        }
    }
}