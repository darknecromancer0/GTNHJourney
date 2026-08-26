package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.gtnhjourney.research.ResearchKey;

/** Pure ordering policy for Journey item-panel modes. Input timelines are oldest first. */
public final class JourneyPanelOrder {

    private JourneyPanelOrder() {}

    /** Legacy/fallback entrypoint. Without activity data N falls back to full research chronology, never a truncated set. */
    static List<ResearchKey> keysForMode(List<ResearchKey> researchOldestFirst, JourneyViewState.Mode mode, int ignored) {
        return keysForMode(researchOldestFirst, researchOldestFirst, mode);
    }

    static List<ResearchKey> keysForMode(
        List<ResearchKey> researchOldestFirst,
        List<ResearchKey> activityOldestFirst,
        JourneyViewState.Mode mode) {
        if (researchOldestFirst == null || researchOldestFirst.isEmpty() || mode == null
            || mode == JourneyViewState.Mode.ALL) return Collections.emptyList();

        List<ResearchKey> researchNewestFirst = reversedUnique(researchOldestFirst);
        if (mode != JourneyViewState.Mode.NEWEST) return researchNewestFirst;

        Set<ResearchKey> researched = new HashSet<ResearchKey>(researchNewestFirst);
        List<ResearchKey> ordered = new ArrayList<ResearchKey>(researchNewestFirst.size());
        Set<ResearchKey> seen = new HashSet<ResearchKey>();
        if (activityOldestFirst != null) {
            for (int i = activityOldestFirst.size() - 1; i >= 0; i--) {
                ResearchKey key = activityOldestFirst.get(i);
                if (key != null && researched.contains(key) && seen.add(key)) ordered.add(key);
            }
        }
        // Corrupt/legacy partial activity data must never make N lose an item that is visible in J. Missing entries are
        // deliberately placed after known activity rather than pretending they were recently touched.
        for (ResearchKey key : researchNewestFirst) if (seen.add(key)) ordered.add(key);
        return Collections.unmodifiableList(ordered);
    }

    private static List<ResearchKey> reversedUnique(List<ResearchKey> oldestFirst) {
        List<ResearchKey> out = new ArrayList<ResearchKey>(oldestFirst.size());
        Set<ResearchKey> seen = new HashSet<ResearchKey>();
        for (int i = oldestFirst.size() - 1; i >= 0; i--) {
            ResearchKey key = oldestFirst.get(i);
            if (key != null && seen.add(key)) out.add(key);
        }
        return Collections.unmodifiableList(out);
    }
}
