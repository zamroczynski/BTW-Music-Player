package btw.community.btwmusicplayer.mixin;

import btw.community.btwmusicplayer.*;
import btw.community.btwmusicplayer.CombatTracker;
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

    private static final int DEBUG_LOG_INTERVAL_TICKS = 100; // Log every ~5 seconds
    private static int tickCounter = 0;

    private CombatTracker combatTracker;
    private ConditionEvaluator conditionEvaluator;
    private PlaylistManager playlistManager;
    private PlaybackStateMachine playbackStateMachine;

    private void initializeComponents() {
        if (this.combatTracker == null) {
            this.combatTracker = new CombatTracker();
            this.conditionEvaluator = new ConditionEvaluator();
            this.playlistManager = new PlaylistManager();
            this.playbackStateMachine = new PlaybackStateMachine(this.sndSystem, this.options);
            MusicLogger.always("Music Player components initialized.");
        }
    }

    @Inject(method = "playRandomMusicIfReady", at = @At("HEAD"), cancellable = true)
    private void onPlayRandomMusic(CallbackInfo ci) {
        ci.cancel();
        if (this.sndSystem == null || this.options == null || this.options.musicVolume == 0.0f) {
            return;
        }

        initializeComponents();

        tickCounter++;
        boolean shouldLog = tickCounter % DEBUG_LOG_INTERVAL_TICKS == 0;

        Minecraft mc = Minecraft.getMinecraft();

        // 1. Update game state trackers
        combatTracker.update(mc, btwmusicplayerAddon.getMusicContext());

        // 2. Determine the correct playlist based on the current context
        playlistManager.update(conditionEvaluator, combatTracker, mc, shouldLog);

        // 3. Handle song playback logic
        if (!this.sndSystem.playing("BgMusic") && playbackStateMachine != null && playlistManager.getCurrentSongRule() != null) {
            MusicLogger.log("[SoundManager] Current song finished. Advancing playlist.");
            playlistManager.advanceToNextSong();
        }

        SongRule targetSong = playlistManager.getCurrentSongRule();
        playbackStateMachine.update(targetSong, shouldLog);
    }
}