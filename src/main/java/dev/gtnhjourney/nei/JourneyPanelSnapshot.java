package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.gtnhjourney.research.ResearchKey;

/** Pure Journey-panel key planning: deduplicate timelines before applying the active view order. */
final class JourneyPanelSnapshot {

    private JourneyPanelSnapshot() {}

    static List<ResearchKey> keys(List<ResearchKey> researchOldestFirst, JourneyViewState.Mode mode, int ignored) {
        return keys(researchOldestFirst, researchOldestFirst, mode);
    }

    static List<ResearchKey> keys(
        List<ResearchKey> researchOldestFirst,
        List<ResearchKey> activityOldestFirst,
        JourneyViewState.Mode mode) {
        List<ResearchKey> research = unique(researchOldestFirst);
        if (research.isEmpty()) return Collections.emptyList();
        return JourneyPanelOrder.keysForMode(research, unique(activityOldestFirst), mode);
    }

    private static List<ResearchKey> unique(List<ResearchKey> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        Set<ResearchKey> unique = new LinkedHashSet<ResearchKey>();
        for (ResearchKey key : source) if (key != null) unique.add(key);
        return new ArrayList<ResearchKey>(unique);
    }
}
