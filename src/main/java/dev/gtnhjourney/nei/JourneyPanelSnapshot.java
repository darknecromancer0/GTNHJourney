package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.gtnhjourney.research.ResearchKey;

/** Pure Journey-panel key planning: deduplicate an oldest-first timeline before applying the active view order. */
final class JourneyPanelSnapshot {

    private JourneyPanelSnapshot() {}

    static List<ResearchKey> keys(List<ResearchKey> oldestFirst, JourneyViewState.Mode mode, int newestLimit) {
        if (oldestFirst == null || oldestFirst.isEmpty()) return Collections.emptyList();
        Set<ResearchKey> unique = new LinkedHashSet<ResearchKey>();
        for (ResearchKey key : oldestFirst) {
            if (key != null) unique.add(key);
        }
        if (unique.isEmpty()) return Collections.emptyList();
        return JourneyPanelOrder.keysForMode(new ArrayList<ResearchKey>(unique), mode, newestLimit);
    }
}
