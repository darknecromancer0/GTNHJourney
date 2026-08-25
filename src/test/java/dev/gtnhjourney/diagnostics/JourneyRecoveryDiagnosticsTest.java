package dev.gtnhjourney.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneySnapshotData;
import dev.gtnhjourney.recovery.DeletionRecord;
import dev.gtnhjourney.recovery.JourneySnapshot;
import dev.gtnhjourney.recovery.JourneySnapshotService;
import dev.gtnhjourney.recovery.ResearchEntrySnapshot;
import dev.gtnhjourney.recovery.ResearchStateSnapshot;
import dev.gtnhjourney.recovery.ResearchTransaction;
import dev.gtnhjourney.recovery.SnapshotKind;
import dev.gtnhjourney.research.ResearchKey;

public class JourneyRecoveryDiagnosticsTest {

    @Test
    public void captureReportsRecoveryAndSnapshotStateWithoutMutatingIt() {
        UUID player = UUID.randomUUID();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        JourneySnapshotData snapshots = new JourneySnapshotData();
        JourneySnapshotService snapshotService = new JourneySnapshotService(snapshots);

        ResearchEntrySnapshot entry = new ResearchEntrySnapshot(new ResearchKey("minecraft:stone", 0, ""), null, 0);
        recovery.pushUndo(
            player,
            new ResearchTransaction(
                11L,
                100L,
                "delete exact",
                Collections.<ResearchEntrySnapshot>emptyList(),
                Collections.singletonList(entry)));
        recovery.pushRedo(
            player,
            new ResearchTransaction(
                12L,
                200L,
                "undo delete exact",
                Collections.singletonList(entry),
                Collections.<ResearchEntrySnapshot>emptyList()));
        recovery.appendDeletion(player, new DeletionRecord(21L, 150L, entry, true));
        recovery.appendDeletion(player, new DeletionRecord(22L, 160L, entry, false));

        ResearchStateSnapshot state = new ResearchStateSnapshot(Collections.singletonList(entry));
        snapshots.add(player, new JourneySnapshot(31L, "auto-31", 2400L, SnapshotKind.AUTO, state));
        snapshots.add(player, new JourneySnapshot(32L, "safety-32", 2500L, SnapshotKind.SAFETY, state));
        snapshots.add(player, new JourneySnapshot(33L, "manual-save", 2600L, SnapshotKind.MANUAL, state));

        JourneyRecoveryDiagnostics.Snapshot diagnostic = JourneyRecoveryDiagnostics.capture(
            recovery,
            snapshots,
            snapshotService,
            player);

        assertEquals(1, diagnostic.getUndoDepth());
        assertEquals(1, diagnostic.getRedoDepth());
        assertEquals(2, diagnostic.getDeletionCount());
        assertEquals(1, diagnostic.getActiveDeletionCount());
        assertEquals(2, diagnostic.getRotatingSnapshotCount());
        assertEquals(1, diagnostic.getManualSnapshotCount());
        assertEquals("manual-save", diagnostic.getLatestSnapshotName());
        assertEquals("undo delete exact", diagnostic.getLastTransactionDescription());
        assertEquals(0L, diagnostic.getSkippedSuspiciousSnapshots());

        assertEquals(1, recovery.undoDepth(player));
        assertEquals(1, recovery.redoDepth(player));
    }

    @Test
    public void captureAcceptsLongLivedRuntimeSkipCounter() {
        UUID player = UUID.randomUUID();
        JourneyRecoveryDiagnostics.Snapshot diagnostic = JourneyRecoveryDiagnostics.capture(
            new JourneyRecoveryData(),
            new JourneySnapshotData(),
            7L,
            player);

        assertEquals(7L, diagnostic.getSkippedSuspiciousSnapshots());
    }
}
