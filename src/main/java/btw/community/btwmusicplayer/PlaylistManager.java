package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Manages the selection and lifecycle of music playlists.
 * It determines the best playlist based on current game conditions and manages the currently playing track.
 */
public class PlaylistManager {
    private List<SongRule> currentPlaylist = new ArrayList<>();
    private int currentPlaylistIndex = 0;
    private SongRule currentSongRule = null;
    private int currentPriority = -1;
    private List<SongRule> pendingPlaylist = null;
    private long pendingPlaylistTimestamp = 0;

    public void update(ConditionEvaluator evaluator, CombatTracker combatTracker, Minecraft mc, boolean log) {
        List<SongRule> newlyFoundPlaylist = findBestMatchingRules(evaluator, combatTracker, mc, log);
        int newlyFoundPriority = getPriority(newlyFoundPlaylist);

        int delaySeconds = ModConfig.getInstance().contextChangeDelaySeconds;

        if (delaySeconds == 0) {
            if (!arePlaylistsEqual(newlyFoundPlaylist, this.currentPlaylist)) {
                if (log) MusicLogger.log("[PlaylistManager] Context changed (delay disabled). Creating new playlist.");
                commitChange(newlyFoundPlaylist, newlyFoundPriority);
            }
            return;
        }

        // If the new playlist has a higher priority, switch immediately, ignoring any delays.
        if (newlyFoundPriority > this.currentPriority) {
            MusicLogger.log("[PlaylistManager] High-priority override detected! Old: " + this.currentPriority + ", New: " + newlyFoundPriority + ". Changing music immediately.");
            commitChange(newlyFoundPlaylist, newlyFoundPriority);
            return;
        }

        // If context returned to what's currently playing, cancel any pending change.
        if (arePlaylistsEqual(newlyFoundPlaylist, this.currentPlaylist)) {
            if (this.pendingPlaylist != null) {
                MusicLogger.log("[PlaylistManager] Context returned to current playlist. Cancelling pending change.");
                this.pendingPlaylist = null;
                this.pendingPlaylistTimestamp = 0;
            }
            return;
        }

        // If the newly found context is the same as the one we are waiting for, check the timer.
        if (arePlaylistsEqual(newlyFoundPlaylist, this.pendingPlaylist)) {
            long elapsed = System.currentTimeMillis() - this.pendingPlaylistTimestamp;
            if (elapsed > delaySeconds * 1000L) {
                MusicLogger.log("[PlaylistManager] Context has been stable for " + delaySeconds + "s. Committing change.");
                commitChange(this.pendingPlaylist, getPriority(this.pendingPlaylist));
            } else {
                if (log) MusicLogger.log("[PlaylistManager] Context is stable but waiting for timer (" + (elapsed / 1000L) + "s / " + delaySeconds + "s).");
            }
        } else {
            // The context has changed to something new. Start the timer for this new context.
            MusicLogger.log("[PlaylistManager] Context changed. Starting " + delaySeconds + "s timer for new playlist.");
            this.pendingPlaylist = newlyFoundPlaylist;
            this.pendingPlaylistTimestamp = System.currentTimeMillis();
        }
    }

    private void commitChange(List<SongRule> newPlaylist, int newPriority) {
        this.currentPlaylist = newPlaylist;
        this.currentPriority = newPriority;
        if (!this.currentPlaylist.isEmpty()) {
            Collections.shuffle(this.currentPlaylist);
        }
        this.currentPlaylistIndex = 0;
        this.currentSongRule = this.currentPlaylist.isEmpty() ? null : this.currentPlaylist.get(0);

        this.pendingPlaylist = null;
        this.pendingPlaylistTimestamp = 0;

        MusicLogger.log("[PlaylistManager] New playlist committed. Priority: " + newPriority + ", Songs: " + newPlaylist.size());
    }

    private int getPriority(List<SongRule> rules) {
        if (rules == null || rules.isEmpty()) return -1;

        int highestPriority = -1;
        for (SongRule rule : rules) {
            if (rule.priority > highestPriority) {
                highestPriority = rule.priority;
            }
        }
        return highestPriority;
    }

    private List<SongRule> findBestMatchingRules(ConditionEvaluator evaluator, CombatTracker combatTracker, Minecraft mc, boolean log) {
        List<SongRule> potentialRules = new ArrayList<>();
        for (SongRule rule : MusicManager.getSongRules()) {
            if (evaluator.check(rule.conditions, mc, combatTracker, false)) { // Logging disabled for individual checks
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

    public void advanceToNextSong() {
        if (this.currentPlaylist.isEmpty()) {
            this.currentSongRule = null;
            return;
        }
        this.currentPlaylistIndex = (this.currentPlaylistIndex + 1) % this.currentPlaylist.size();
        this.currentSongRule = this.currentPlaylist.get(this.currentPlaylistIndex);
        MusicLogger.log("[PlaylistManager] Advancing to next song: " + (this.currentSongRule != null ? this.currentSongRule.file : "none"));
    }

    public SongRule getCurrentSongRule() {
        return this.currentSongRule;
    }

    /**
     * Checks if a context change is waiting for its timer to complete.
     */
    public boolean hasPendingChange() {
        return this.pendingPlaylist != null;
    }

    /**
     * Immediately commits the pending playlist change. Used when a song ends naturally
     * to prevent playing another song from the old, outdated playlist.
     */
    public void forceCommitPendingChange() {
        if (this.pendingPlaylist != null) {
            MusicLogger.log("[PlaylistManager] Forcing commit of pending change due to song ending.");
            commitChange(this.pendingPlaylist, getPriority(this.pendingPlaylist));
        }
    }

    private boolean arePlaylistsEqual(List<SongRule> list1, List<SongRule> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }
}