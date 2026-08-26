package dev.gtnhjourney.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.recovery.JourneySnapshot;
import dev.gtnhjourney.recovery.ResearchStateSnapshot;
import dev.gtnhjourney.recovery.SnapshotKind;

public class RecoveryDiagnosticSummaryTest {

    @Test
    public void summaryReportsCurrentRecoveryDepthsAndSnapshotRingState() {
        List<JourneySnapshot> snapshots = Arrays.asList(
            new JourneySnapshot(10L, "auto-a", 100L, SnapshotKind.AUTO, new ResearchStateSnapshot(null)),
            new JourneySnapshot(12L, "manual-b", 170L, SnapshotKind.MANUAL, new ResearchStateSnapshot(null)),
            new JourneySnapshot(11L, "safety-c", 150L, SnapshotKind.SAFETY, new ResearchStateSnapshot(null)));

        List<String> lines = RecoveryDiagnosticSummary.lines(4, 2, 3, 8, snapshots, 200L, 5L);

        assertTrue(lines.contains("undoDepth=4"));
        assertTrue(lines.contains("redoDepth=2"));
        assertTrue(lines.contains("deletedActive=3"));
        assertTrue(lines.contains("deletedTotal=8"));
        assertTrue(lines.contains("rotatingSnapshots=2"));
        assertTrue(lines.contains("manualSnapshots=1"));
        assertTrue(lines.contains("newestSnapshotId=12"));
        assertTrue(lines.contains("newestSnapshotName=manual-b"));
        assertTrue(lines.contains("newestSnapshotKind=MANUAL"));
        assertTrue(lines.contains("newestSnapshotAgeTicks=30"));
        assertTrue(lines.contains("skippedSuspiciousAutoSnapshots=5"));
    }
}
