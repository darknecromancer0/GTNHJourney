package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class JourneyPanelSnapshotTest {

    private final ResearchKey oldest = new ResearchKey("test:oldest", 0, "");
    private final ResearchKey middle = new ResearchKey("test:middle", 0, "");
    private final ResearchKey newest = new ResearchKey("test:newest", 0, "");

    @Test
    public void researchedReturnsAllUniqueKeysNewestFirst() {
        List<ResearchKey> input = Arrays.asList(oldest, null, middle, oldest, newest);
        assertEquals(
            Arrays.asList(newest, middle, oldest),
            JourneyPanelSnapshot.keys(input, JourneyViewState.Mode.RESEARCHED, 2));
    }

    @Test
    public void fallbackNReturnsTheSameUniqueResearchSet() {
        List<ResearchKey> input = Arrays.asList(oldest, middle, oldest, newest);
        assertEquals(
            Arrays.asList(newest, middle, oldest),
            JourneyPanelSnapshot.keys(input, JourneyViewState.Mode.NEWEST, 64));
    }

    @Test
    public void allHasNoJourneyOwnedSnapshot() {
        assertEquals(
            Collections.emptyList(),
            JourneyPanelSnapshot.keys(Arrays.asList(oldest, newest), JourneyViewState.Mode.ALL, 64));
    }
}
