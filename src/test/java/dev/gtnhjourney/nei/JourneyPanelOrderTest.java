package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class JourneyPanelOrderTest {

    @Test
    public void researchedKeepsTheWholeResearchSet() {
        ResearchKey oldest = new ResearchKey("test:oldest", 0, "");
        ResearchKey middle = new ResearchKey("test:middle", 0, "");
        ResearchKey newest = new ResearchKey("test:newest", 0, "");
        List<ResearchKey> unlockOrder = Arrays.asList(oldest, middle, newest);

        assertEquals(
            Arrays.asList(newest, middle, oldest),
            JourneyPanelOrder.keysForMode(unlockOrder, JourneyViewState.Mode.RESEARCHED, 2));
    }

    @Test
    public void allModeDoesNotProduceJourneyOwnedOrdering() {
        ResearchKey only = new ResearchKey("test:only", 0, "");
        assertEquals(
            Collections.emptyList(),
            JourneyPanelOrder.keysForMode(Collections.singletonList(only), JourneyViewState.Mode.ALL, 64));
    }
}
