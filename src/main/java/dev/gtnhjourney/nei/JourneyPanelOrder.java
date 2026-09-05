package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.gtnhjourney.research.ResearchKey;

/** Legacy canonical Journey ordering. 1.1.26 sorting is composed later by JourneySortPlanner. */
public final class JourneyPanelOrder {

    private JourneyPanelOrder() {}

    static List<ResearchKey> keysForMode(List<ResearchKey> researchOldestFirst, JourneyViewState.Mode mode, int ignored) {
        return keysForMode(researchOldestFirst, researchOldestFirst, mode);
    }

    static List<ResearchKey> keysForMode(
        List<ResearchKey> researchOldestFirst,
        List<ResearchKey> activityOldestFirst,
        JourneyViewState.Mode mode) {
        if (researchOldestFirst == null || researchOldestFirst.isEmpty() || mode == null
            || mode == JourneyViewState.Mode.ALL) return Collections.emptyList();
        return reversedUnique(researchOldestFirst);
    }

    static List<ResearchKey> reversedUnique(List<ResearchKey> oldestFirst) {
        List<ResearchKey> out = new ArrayList<ResearchKey>(oldestFirst.size());
        Set<ResearchKey> seen = new HashSet<ResearchKey>();
        for (int i = oldestFirst.size() - 1; i >= 0; i--) {
            ResearchKey key = oldestFirst.get(i);
            if (key != null && seen.add(key)) out.add(key);
        }
        return Collections.unmodifiableList(out);
    }
}
