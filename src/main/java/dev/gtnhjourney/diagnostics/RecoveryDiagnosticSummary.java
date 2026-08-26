package dev.gtnhjourney.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.gtnhjourney.recovery.JourneySnapshot;
import dev.gtnhjourney.recovery.SnapshotKind;

/** Pure formatter for the persistent recovery/snapshot state exposed in /journey dump. */
public final class RecoveryDiagnosticSummary {

    private RecoveryDiagnosticSummary() {}

    public static List<String> lines(
        int undoDepth,
        int redoDepth,
        int activeDeleted,
        int totalDeleted,
        List<JourneySnapshot> snapshots,
        long currentWorldTick,
        long skippedSuspiciousAutoSnapshots) {
        int rotating = 0;
        int manual = 0;
        JourneySnapshot newest = null;
        if (snapshots != null) {
            for (JourneySnapshot snapshot : snapshots) {
                if (snapshot == null) continue;
                if (snapshot.kind() == SnapshotKind.MANUAL) manual++;
                else rotating++;
                if (newest == null || snapshot.id() > newest.id()) newest = snapshot;
            }
        }

        List<String> out = new ArrayList<String>();
        out.add("undoDepth=" + Math.max(0, undoDepth));
        out.add("redoDepth=" + Math.max(0, redoDepth));
        out.add("deletedActive=" + Math.max(0, activeDeleted));
        out.add("deletedTotal=" + Math.max(0, totalDeleted));
        out.add("rotatingSnapshots=" + rotating);
        out.add("manualSnapshots=" + manual);
        if (newest == null) {
            out.add("newestSnapshotId=none");
            out.add("newestSnapshotName=none");
            out.add("newestSnapshotKind=none");
            out.add("newestSnapshotAgeTicks=-1");
        } else {
            out.add("newestSnapshotId=" + newest.id());
            out.add("newestSnapshotName=" + newest.name());
            out.add("newestSnapshotKind=" + newest.kind().name());
            out.add("newestSnapshotAgeTicks=" + Math.max(0L, currentWorldTick - newest.worldTick()));
        }
        out.add("skippedSuspiciousAutoSnapshots=" + Math.max(0L, skippedSuspiciousAutoSnapshots));
        return Collections.unmodifiableList(out);
    }
}
