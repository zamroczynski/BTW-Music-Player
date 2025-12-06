package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.Minecraft;

import java.util.*;

/**
 * Manages the selection and lifecycle of music playlists.
 */
public class PlaylistManager {
    private List<SongRule> currentPlaylist = new ArrayList<>();
    private int currentPlaylistIndex = 0;
    private SongRule currentSongRule = null;
    private int currentPriority = -1;
    private List<SongRule> pendingPlaylist = null;
    private long pendingPlaylistTimestamp = 0;

    public void update(ConditionEvaluator evaluator, CombatTracker combatTracker, Minecraft mc, boolean logInterval) {
        boolean shouldTrace = ModConfig.getInstance().enableDebugLogging && logInterval;

        evaluator.updateBiomeState(mc, shouldTrace);

        List<SongRule> newlyFoundPlaylist = findBestMatchingRules(evaluator, combatTracker, mc, shouldTrace);
        int newlyFoundPriority = getPriority(newlyFoundPlaylist);

        int delaySeconds = ModConfig.getInstance().contextChangeDelaySeconds;

        if (delaySeconds == 0) {
            if (!arePlaylistsEqual(newlyFoundPlaylist, this.currentPlaylist)) {
                if (logInterval) MusicLogger.log("[PlaylistManager] Context changed (delay disabled). Creating new playlist.");
                commitChange(newlyFoundPlaylist, newlyFoundPriority);
            }
            return;
        }

        if (newlyFoundPriority > this.currentPriority) {
            MusicLogger.log("[PlaylistManager] High-priority override detected! Old: " + this.currentPriority + ", New: " + newlyFoundPriority + ". Changing music immediately.");
            commitChange(newlyFoundPlaylist, newlyFoundPriority);
            return;
        }

        if (arePlaylistsEqual(newlyFoundPlaylist, this.currentPlaylist)) {
            if (this.pendingPlaylist != null) {
                MusicLogger.log("[PlaylistManager] Context returned to current playlist. Cancelling pending change.");
                this.pendingPlaylist = null;
                this.pendingPlaylistTimestamp = 0;
            }
            return;
        }

        if (arePlaylistsEqual(newlyFoundPlaylist, this.pendingPlaylist)) {
            long elapsed = System.currentTimeMillis() - this.pendingPlaylistTimestamp;
            if (elapsed > delaySeconds * 1000L) {
                MusicLogger.log("[PlaylistManager] Context has been stable for " + delaySeconds + "s. Committing change.");
                commitChange(this.pendingPlaylist, getPriority(this.pendingPlaylist));
            } else {
                if (logInterval) MusicLogger.log("[PlaylistManager] Context is stable but waiting for timer (" + (elapsed / 1000L) + "s / " + delaySeconds + "s).");
            }
        } else {
            MusicLogger.log("[PlaylistManager] Context changed (Priority " + newlyFoundPriority + "). Starting " + delaySeconds + "s timer.");
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
        if (this.currentSongRule != null) {
            MusicLogger.log("[PlaylistManager] First song in playlist: " + this.currentSongRule.file);
        } else {
            MusicLogger.log("[PlaylistManager] Playlist is EMPTY. Silence.");
        }
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

    private List<SongRule> findBestMatchingRules(ConditionEvaluator evaluator, CombatTracker combatTracker, Minecraft mc, boolean trace) {
        List<SongRule> potentialRules = new ArrayList<>();
        Map<String, Integer> failureStats = trace ? new HashMap<>() : null;

        if (trace) MusicLogger.trace("--- Evaluating Rules ---");

        for (SongRule rule : MusicManager.getSongRules()) {
            if (evaluator.check(rule.conditions, mc, combatTracker, failureStats)) {
                potentialRules.add(rule);
            }
        }

        if (trace && failureStats != null && !failureStats.isEmpty()) {
            MusicLogger.trace("--- Skipped Songs Summary ---");
            for (Map.Entry<String, Integer> entry : failureStats.entrySet()) {
                MusicLogger.trace("   Skipped due to: " + entry.getKey() + " -> Count: " + entry.getValue());
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

        if (trace) {
            if (!bestPlaylist.isEmpty()) {
                MusicLogger.trace("--- Best Match Found: Priority " + bestPriority + ", Count: " + bestPlaylist.size() + " ---");
            } else {
                MusicLogger.trace("--- No Matching Rules Found (Silence) ---");
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

    public boolean hasPendingChange() {
        return this.pendingPlaylist != null;
    }

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