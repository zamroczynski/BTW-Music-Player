package btw.community.btwmusicplayer;

import btw.community.btwmusicplayer.data.SongRule;
import net.minecraft.src.Minecraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Manages "Overlay" tracks (e.g., heartbeat, geiger counter) that play simultaneously
 * with the background music.
 */
public class OverlayManager {

    private SongRule currentOverlayRule = null;

    public void update(ConditionEvaluator evaluator, MusicCombatTracker combatTracker, Minecraft mc, boolean logInterval) {
        SongRule bestMatch = findBestMatchingOverlay(evaluator, combatTracker, mc, logInterval);

        if (bestMatch != this.currentOverlayRule) {
            if (logInterval || ModConfig.getInstance().enableDebugLogging) {
                String oldFile = (this.currentOverlayRule != null) ? this.currentOverlayRule.file : "None";
                String newFile = (bestMatch != null) ? bestMatch.file : "None";
                MusicLogger.log("[OverlayManager] Change detected: " + oldFile + " -> " + newFile);
            }
            this.currentOverlayRule = bestMatch;
        }
    }

    public SongRule getCurrentOverlayRule() {
        return this.currentOverlayRule;
    }

    private SongRule findBestMatchingOverlay(ConditionEvaluator evaluator, MusicCombatTracker combatTracker, Minecraft mc, boolean trace) {
        List<SongRule> candidates = new ArrayList<>();
        Map<String, Integer> failureStats = trace ? new HashMap<>() : null;

        for (SongRule rule : MusicManager.getSongRules()) {
            if (rule.isOverlayRule()) {
                if (evaluator.check(rule.conditions, mc, combatTracker, failureStats)) {
                    candidates.add(rule);
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        SongRule best = null;
        for (SongRule rule : candidates) {
            if (best == null || rule.priority > best.priority) {
                best = rule;
            }
        }

        return best;
    }
}