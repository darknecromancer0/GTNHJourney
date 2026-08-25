package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneySnapshotData;
import dev.gtnhjourney.research.ResearchKey;

public class JourneySnapshotServiceTest {

    @Test
    public void autoSnapshotsUse2400TickCadenceSuppressUnchangedAndGuardSuspiciousDrops() {
        UUID player = UUID.randomUUID();
        JourneySnapshotData data = new JourneySnapshotData();
        JourneySnapshotService service = new JourneySnapshotService(data);
        ResearchStateSnapshot hundred = state(100);

        assertFalse(service.maybeAutoSnapshot(player, 2399L, true, hundred));
        assertTrue(service.maybeAutoSnapshot(player, 2400L, true, hundred));
        assertFalse(service.maybeAutoSnapshot(player, 4800L, true, hundred));

        ResearchStateSnapshot hundredOne = state(101);
        assertTrue(service.maybeAutoSnapshot(player, 4800L, true, hundredOne));
        assertFalse(service.maybeAutoSnapshot(player, 7200L, true, state(20)));
        assertEquals(1L, service.skippedSuspiciousSnapshots());

        assertTrue(service.createManual(player, "low-manual", 7200L, state(20)) != null);
        assertEquals(1, data.manualSnapshots(player).size());
    }

    @Test
    public void safetyAndAutoShareTwentySlotRingWhileManualUsesTen() {
        UUID player = UUID.randomUUID();
        JourneySnapshotData data = new JourneySnapshotData();
        JourneySnapshotService service = new JourneySnapshotService(data);

        for (int i = 0; i < 22; i++) service.createSafety(player, "safety-" + i, i, state(i + 1));
        for (int i = 0; i < 12; i++) service.createManual(player, "manual-" + i, i, state(i + 1));

        assertEquals(20, data.rotatingSnapshots(player).size());
        assertEquals("safety-2", data.rotatingSnapshots(player).get(0).name());
        assertEquals(10, data.manualSnapshots(player).size());
        assertEquals("manual-2", data.manualSnapshots(player).get(0).name());
    }

    private static ResearchStateSnapshot state(int size) {
        List<ResearchEntrySnapshot> entries = new ArrayList<ResearchEntrySnapshot>();
        for (int i = 0; i < size; i++) {
            entries.add(new ResearchEntrySnapshot(new ResearchKey("test:item_" + i, 0, ""), null, i));
        }
        return new ResearchStateSnapshot(entries);
    }
}
