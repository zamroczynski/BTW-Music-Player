package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.Minecraft;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the selection and lifecycle of music playlists.
 * UPDATED: Uses a stateful "Global Shuffle" approach.
 */
public class PlaylistManager {
    private List<SongRule> currentContextPlaylist = new ArrayList<>();

    private List<SongRule> pendingContextPlaylist = null;

    private SongRule currentSongRule = null;
    private int currentPriority = -1;

    private long pendingPlaylistTimestamp = 0;

    public void update(ConditionEvaluator evaluator, MusicCombatTracker combatTracker, Minecraft mc, boolean logInterval) {
        boolean shouldTrace = ModConfig.getInstance().enableDebugLogging && logInterval;

        evaluator.updateBiomeState(mc, shouldTrace);

        List<SongRule> newlyFoundPlaylist = findActiveRules(evaluator, combatTracker, mc, shouldTrace);
        int newlyFoundPriority = getPriority(newlyFoundPlaylist);

        int delaySeconds = ModConfig.getInstance().contextChangeDelaySeconds;

        if (delaySeconds == 0) {
            if (!areContextsEqual(newlyFoundPlaylist, this.currentContextPlaylist)) {
                if (logInterval) MusicLogger.log("[PlaylistManager] Context changed (delay disabled). Switching playlist.");
                commitChange(newlyFoundPlaylist, newlyFoundPriority);
            }
            return;
        }

        if (newlyFoundPriority > this.currentPriority) {
            MusicLogger.log("[PlaylistManager] High-priority override! Old: " + this.currentPriority + ", New: " + newlyFoundPriority + ". Immediate switch.");
            commitChange(newlyFoundPlaylist, newlyFoundPriority);
            return;
        }

        if (areContextsEqual(newlyFoundPlaylist, this.currentContextPlaylist)) {
            if (this.pendingContextPlaylist != null) {
                MusicLogger.log("[PlaylistManager] Context returned to current active state. Cancelling pending change.");
                this.pendingContextPlaylist = null;
                this.pendingPlaylistTimestamp = 0;
            }
            return;
        }

        if (areContextsEqual(newlyFoundPlaylist, this.pendingContextPlaylist)) {
            long elapsed = System.currentTimeMillis() - this.pendingPlaylistTimestamp;
            if (elapsed > delaySeconds * 1000L) {
                MusicLogger.log("[PlaylistManager] Context stable for " + delaySeconds + "s. Committing change.");
                commitChange(this.pendingContextPlaylist, getPriority(this.pendingContextPlaylist));
            } else {
                if (logInterval) MusicLogger.log("[PlaylistManager] Waiting for timer (" + (elapsed / 1000L) + "s / " + delaySeconds + "s).");
            }
        } else {
            MusicLogger.log("[PlaylistManager] Context change detected (Priority " + newlyFoundPriority + "). Starting " + delaySeconds + "s timer.");
            this.pendingContextPlaylist = newlyFoundPlaylist;
            this.pendingPlaylistTimestamp = System.currentTimeMillis();
        }
    }

    private void commitChange(List<SongRule> newPlaylist, int newPriority) {
        this.currentContextPlaylist = newPlaylist;
        this.currentPriority = newPriority;

        this.pendingContextPlaylist = null;
        this.pendingPlaylistTimestamp = 0;

        MusicLogger.log("[PlaylistManager] New Context Committed. Matches found: " + newPlaylist.size() + " (Priority: " + newPriority + ")");

        advanceToNextSong();
    }

    public void advanceToNextSong() {
        if (this.currentContextPlaylist == null || this.currentContextPlaylist.isEmpty()) {
            this.currentSongRule = null;
            MusicLogger.trace("[PlaylistManager] Playlist empty. Setting current song to NULL.");
            return;
        }

        SongRule candidate = null;
        for (SongRule rule : this.currentContextPlaylist) {
            if (!rule.hasBeenPlayed) {
                candidate = rule;
                break;
            }
        }

        if (candidate == null) {
            MusicLogger.log("[PlaylistManager] All songs for this context have been played! Resetting loop (keeping order).");
            for (SongRule rule : this.currentContextPlaylist) {
                rule.hasBeenPlayed = false;
            }
            candidate = this.currentContextPlaylist.get(0);
        }

        this.currentSongRule = candidate;
        if (this.currentSongRule != null) {
            this.currentSongRule.hasBeenPlayed = true;
            MusicLogger.log("[PlaylistManager] Selected Next Song: " + this.currentSongRule.file + " (Marked as Played)");
        }
    }

    private List<SongRule> findActiveRules(ConditionEvaluator evaluator, MusicCombatTracker combatTracker, Minecraft mc, boolean trace) {
        List<SongRule> potentialRules = new ArrayList<>();
        Map<String, Integer> failureStats = trace ? new HashMap<>() : null;

        for (SongRule rule : MusicManager.getSongRules()) {
            if (rule.isOverlayRule()) continue;

            if (evaluator.check(rule.conditions, mc, combatTracker, failureStats)) {
                potentialRules.add(rule);
            }
        }

//        if (trace && failureStats != null && !failureStats.isEmpty()) {
//             MusicLogger.trace("Skipped rules stats: " + failureStats.toString());
//        }w

        int bestPriority = -1;
        for (SongRule rule : potentialRules) {
            if (rule.priority > bestPriority) {
                bestPriority = rule.priority;
            }
        }

        List<SongRule> finalContextList = new ArrayList<>();
        if (bestPriority != -1) {
            for (SongRule rule : potentialRules) {
                if (rule.priority == bestPriority) {
                    finalContextList.add(rule);
                }
            }
        }

//        if (trace && !finalContextList.isEmpty()) {
//            MusicLogger.trace("Active Context Rules found: " + finalContextList.size() + " (Prio: " + bestPriority + ")");
//        }

        return finalContextList;
    }

    private int getPriority(List<SongRule> rules) {
        if (rules == null || rules.isEmpty()) return -1;
        return rules.get(0).priority;
    }

    private boolean areContextsEqual(List<SongRule> list1, List<SongRule> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            if (list1.get(i) != list2.get(i)) return false;
        }
        return true;
    }

    public SongRule getCurrentSongRule() {
        return this.currentSongRule;
    }

    public boolean hasPendingChange() {
        return this.pendingContextPlaylist != null;
    }

    public void forceCommitPendingChange() {
        if (this.pendingContextPlaylist != null) {
            MusicLogger.log("[PlaylistManager] Forcing commit of pending change due to song ending.");
            commitChange(this.pendingContextPlaylist, getPriority(this.pendingContextPlaylist));
        }
    }

    public void forceReset() {
        this.pendingContextPlaylist = null;
        this.pendingPlaylistTimestamp = 0;
        this.currentContextPlaylist.clear();
        this.currentSongRule = null;
        this.currentPriority = -1;

        MusicLogger.log("[PlaylistManager] Forced reset of playlist manager state (Global history kept).");
    }
}