package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.research.ResearchKey;

public class SnapshotDeletionReconciliationTest {

    @SuppressWarnings("unchecked")
    @Test
    public void emptySnapshotReactivatesInactiveDeletionForNowAbsentState() throws Exception {
        UUID player = UUID.randomUUID();
        ResearchKey deleted = new ResearchKey("minecraft:stone", 0, "");
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        recovery.appendDeletion(
            player,
            new DeletionRecord(201L, 1L, new ResearchEntrySnapshot(deleted, null, 0), false));

        Method planner = JourneyMutationService.class.getDeclaredMethod(
            "deletionChangesForTarget",
            JourneyRecoveryData.class,
            UUID.class,
            ResearchStateSnapshot.class);
        planner.setAccessible(true);

        List<DeletionStateChange> changes = (List<DeletionStateChange>) planner.invoke(
            null,
            recovery,
            player,
            new ResearchStateSnapshot(Collections.<ResearchEntrySnapshot>emptyList()));

        assertEquals(1, changes.size());
        assertEquals(201L, changes.get(0).deletionId());
        assertTrue(changes.get(0).activeAfterForward());
    }
}
