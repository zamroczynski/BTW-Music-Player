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

    public void update(ConditionEvaluator evaluator, CombatTracker combatTracker, Minecraft mc, boolean log) {
        List<SongRule> bestRules = findBestMatchingRules(evaluator, combatTracker, mc, log);

        if (!arePlaylistsEqual(bestRules, this.currentPlaylist)) {
            if (log) MusicLogger.log("[PlaylistManager] Context changed. Creating new playlist with " + bestRules.size() + " songs.");
            this.currentPlaylist = bestRules;
            Collections.shuffle(this.currentPlaylist);
            this.currentPlaylistIndex = 0;
            this.currentSongRule = this.currentPlaylist.isEmpty() ? null : this.currentPlaylist.get(0);
        }
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
        MusicLogger.log("[PlaylistManager] Advancing to next song: " + this.currentSongRule.file);
    }

    public SongRule getCurrentSongRule() {
        return this.currentSongRule;
    }

    private boolean arePlaylistsEqual(List<SongRule> list1, List<SongRule> list2) {
        if (list1.size() != list2.size()) return false;
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }
}