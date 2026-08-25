package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.gtnhjourney.research.ResearchKey;

/** Pure ordering policy for Journey item-panel modes. Input is always oldest unlock first. */
public final class JourneyPanelOrder {

    private JourneyPanelOrder() {}

    static List<ResearchKey> keysForMode(List<ResearchKey> oldestFirst, JourneyViewState.Mode mode, int newestLimit) {
        if (oldestFirst == null || oldestFirst.isEmpty() || mode == null || mode == JourneyViewState.Mode.ALL) {
            return Collections.emptyList();
        }

        if (mode == JourneyViewState.Mode.RESEARCHED) {
            return Collections.unmodifiableList(new ArrayList<ResearchKey>(oldestFirst));
        }

        int limit = Math.max(0, newestLimit);
        if (limit == 0) return Collections.emptyList();
        int start = Math.max(0, oldestFirst.size() - limit);
        List<ResearchKey> newestFirst = new ArrayList<ResearchKey>(oldestFirst.size() - start);
        for (int i = oldestFirst.size() - 1; i >= start; i--) {
            ResearchKey key = oldestFirst.get(i);
            if (key != null) newestFirst.add(key);
        }
        return Collections.unmodifiableList(newestFirst);
    }
}
