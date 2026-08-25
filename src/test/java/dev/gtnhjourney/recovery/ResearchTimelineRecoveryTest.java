package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;
import dev.gtnhjourney.research.ResearchTimeline;

public class ResearchTimelineRecoveryTest {

    @Test
    public void removedEntryCanBeReinsertedAtOriginalPosition() {
        ResearchKey a = new ResearchKey("minecraft:stone", 0, "");
        ResearchKey b = new ResearchKey("minecraft:dirt", 0, "");
        ResearchKey c = new ResearchKey("minecraft:glass", 0, "");
        ResearchTimeline timeline = new ResearchTimeline();
        timeline.restore(Arrays.asList(a, b, c));

        assertTrue(timeline.remove(b));
        assertTrue(timeline.insertAt(b, 1));
        assertEquals(Arrays.asList(a, b, c), timeline.snapshotOldestFirst());
        assertFalse(timeline.insertAt(b, 0));
    }
}
