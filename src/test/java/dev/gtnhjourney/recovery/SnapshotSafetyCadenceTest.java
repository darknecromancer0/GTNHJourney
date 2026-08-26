package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneySnapshotData;
import dev.gtnhjourney.research.ResearchKey;

public class SnapshotSafetyCadenceTest {

    @Test
    public void safetySnapshotBecomesTheBaselineForTheNextAutoSnapshot() {
        UUID player = UUID.randomUUID();
        JourneySnapshotData data = new JourneySnapshotData();
        JourneySnapshotService service = new JourneySnapshotService(data);
        ResearchStateSnapshot first = state("test:first");
        ResearchStateSnapshot afterSafety = state("test:after_safety");

        assertTrue(service.maybeAutoSnapshot(player, 2400L, true, first));
        assertNotNull(service.createSafety(player, "before-bulk", 4800L, afterSafety));

        assertFalse(service.maybeAutoSnapshot(player, 4801L, true, afterSafety));
        assertFalse(service.maybeAutoSnapshot(player, 7200L, true, afterSafety));
        assertTrue(service.maybeAutoSnapshot(player, 7200L, true, state("test:new_change")));
    }

    private static ResearchStateSnapshot state(String id) {
        return new ResearchStateSnapshot(
            Arrays.asList(new ResearchEntrySnapshot(new ResearchKey(id, 0, ""), null, 0)));
    }
}
