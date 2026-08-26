package dev.gtnhjourney.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class ResearchActivityTimelineTest {

    @Test
    public void firstResearchAddsButRepeatedOrdinaryObservationDoesNotReorder() {
        ResearchKey log = key("log");
        ResearchKey stone = key("stone");
        ResearchKey potato = key("potato");
        ResearchActivityTimeline timeline = new ResearchActivityTimeline();

        assertTrue(timeline.recordUnlock(log));
        assertTrue(timeline.recordUnlock(stone));
        assertTrue(timeline.recordUnlock(potato));
        assertFalse(timeline.recordUnlock(log));

        assertEquals(Arrays.asList(potato, stone, log), timeline.snapshotNewestFirst());
    }

    @Test
    public void journeyRetrievalMovesExistingResearchToNewestWithoutDuplicatingIt() {
        ResearchKey log = key("log");
        ResearchKey stone = key("stone");
        ResearchKey potato = key("potato");
        ResearchActivityTimeline timeline = new ResearchActivityTimeline();
        timeline.recordUnlock(log);
        timeline.recordUnlock(stone);
        timeline.recordUnlock(potato);

        assertTrue(timeline.recordRetrieval(log));
        assertEquals(Arrays.asList(log, potato, stone), timeline.snapshotNewestFirst());
        assertEquals(3, timeline.size());

        assertFalse(timeline.recordRetrieval(log));
        assertEquals(Arrays.asList(log, potato, stone), timeline.snapshotNewestFirst());
    }

    private static ResearchKey key(String name) {
        return new ResearchKey("test:" + name, 0, "");
    }
}
