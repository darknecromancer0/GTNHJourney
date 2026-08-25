package dev.gtnhjourney.diagnostics;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneySnapshotData;
import dev.gtnhjourney.recovery.JourneySnapshot;
import dev.gtnhjourney.recovery.JourneySnapshotService;

/** Read-only recovery/snapshot diagnostics. Capturing a snapshot never pops or rewrites recovery history. */
public final class JourneyRecoveryDiagnostics {

    private JourneyRecoveryDiagnostics() {}

    public static Snapshot capture(
        JourneyRecoveryData recovery,
        JourneySnapshotData snapshots,
        JourneySnapshotService snapshotService,
        UUID playerId) {
        if (snapshotService == null) return Snapshot.empty();
        return capture(recovery, snapshots, snapshotService.skippedSuspiciousSnapshots(), playerId);
    }

    public static Snapshot capture(
        JourneyRecoveryData recovery,
        JourneySnapshotData snapshots,
        long skippedSuspiciousSnapshots,
        UUID playerId) {
        if (recovery == null || snapshots == null || playerId == null) return Snapshot.empty();

        NBTTagCompound serializedRecovery = new NBTTagCompound();
        recovery.writeToNBT(serializedRecovery);
        TransactionView transactions = findTransactions(serializedRecovery, playerId);

        List<JourneySnapshot> rotating = snapshots.rotatingSnapshots(playerId);
        List<JourneySnapshot> manual = snapshots.manualSnapshots(playerId);
        JourneySnapshot latest = latest(rotating, manual);

        return new Snapshot(
            recovery.undoDepth(playerId),
            recovery.redoDepth(playerId),
            recovery.deletionCount(playerId),
            recovery.activeDeletionCount(playerId),
            rotating.size(),
            manual.size(),
            latest == null ? "" : latest.name(),
            transactions.latestDescription,
            skippedSuspiciousSnapshots);
    }

    private static TransactionView findTransactions(NBTTagCompound root, UUID playerId) {
        NBTTagList players = root.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound player = players.getCompoundTagAt(i);
            UUID stored = new UUID(player.getLong("UuidMost"), player.getLong("UuidLeast"));
            if (!playerId.equals(stored)) continue;
            return latestTransaction(player.getTagList("Undo", 10), player.getTagList("Redo", 10));
        }
        return new TransactionView("", Long.MIN_VALUE, Long.MIN_VALUE);
    }

    private static TransactionView latestTransaction(NBTTagList undo, NBTTagList redo) {
        String description = "";
        long latestTimestamp = Long.MIN_VALUE;
        long latestId = Long.MIN_VALUE;

        TransactionView undoView = latestIn(undo);
        if (isLater(undoView.timestamp, undoView.id, latestTimestamp, latestId)) {
            description = undoView.latestDescription;
            latestTimestamp = undoView.timestamp;
            latestId = undoView.id;
        }

        TransactionView redoView = latestIn(redo);
        if (isLater(redoView.timestamp, redoView.id, latestTimestamp, latestId)) {
            description = redoView.latestDescription;
            latestTimestamp = redoView.timestamp;
            latestId = redoView.id;
        }
        return new TransactionView(description, latestTimestamp, latestId);
    }

    private static TransactionView latestIn(NBTTagList list) {
        String description = "";
        long latestTimestamp = Long.MIN_VALUE;
        long latestId = Long.MIN_VALUE;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound transaction = list.getCompoundTagAt(i);
            long timestamp = transaction.getLong("Timestamp");
            long id = transaction.getLong("Id");
            if (!isLater(timestamp, id, latestTimestamp, latestId)) continue;
            latestTimestamp = timestamp;
            latestId = id;
            description = transaction.getString("Description");
        }
        return new TransactionView(description, latestTimestamp, latestId);
    }

    private static boolean isLater(long timestamp, long id, long currentTimestamp, long currentId) {
        return timestamp > currentTimestamp || timestamp == currentTimestamp && id > currentId;
    }

    private static JourneySnapshot latest(List<JourneySnapshot> rotating, List<JourneySnapshot> manual) {
        JourneySnapshot latest = null;
        if (rotating != null) {
            for (JourneySnapshot snapshot : rotating) latest = later(latest, snapshot);
        }
        if (manual != null) {
            for (JourneySnapshot snapshot : manual) latest = later(latest, snapshot);
        }
        return latest;
    }

    private static JourneySnapshot later(JourneySnapshot left, JourneySnapshot right) {
        if (right == null) return left;
        if (left == null) return right;
        if (right.worldTick() != left.worldTick()) return right.worldTick() > left.worldTick() ? right : left;
        return right.id() > left.id() ? right : left;
    }

    private static final class TransactionView {

        private final String latestDescription;
        private final long timestamp;
        private final long id;

        private TransactionView(String latestDescription, long timestamp, long id) {
            this.latestDescription = latestDescription == null ? "" : latestDescription;
            this.timestamp = timestamp;
            this.id = id;
        }
    }

    public static final class Snapshot {

        private final int undoDepth;
        private final int redoDepth;
        private final int deletionCount;
        private final int activeDeletionCount;
        private final int rotatingSnapshotCount;
        private final int manualSnapshotCount;
        private final String latestSnapshotName;
        private final String lastTransactionDescription;
        private final long skippedSuspiciousSnapshots;

        private Snapshot(
            int undoDepth,
            int redoDepth,
            int deletionCount,
            int activeDeletionCount,
            int rotatingSnapshotCount,
            int manualSnapshotCount,
            String latestSnapshotName,
            String lastTransactionDescription,
            long skippedSuspiciousSnapshots) {
            this.undoDepth = undoDepth;
            this.redoDepth = redoDepth;
            this.deletionCount = deletionCount;
            this.activeDeletionCount = activeDeletionCount;
            this.rotatingSnapshotCount = rotatingSnapshotCount;
            this.manualSnapshotCount = manualSnapshotCount;
            this.latestSnapshotName = latestSnapshotName == null ? "" : latestSnapshotName;
            this.lastTransactionDescription = lastTransactionDescription == null ? "" : lastTransactionDescription;
            this.skippedSuspiciousSnapshots = Math.max(0L, skippedSuspiciousSnapshots);
        }

        private static Snapshot empty() {
            return new Snapshot(0, 0, 0, 0, 0, 0, "", "", 0L);
        }

        public int getUndoDepth() {
            return undoDepth;
        }

        public int getRedoDepth() {
            return redoDepth;
        }

        public int getDeletionCount() {
            return deletionCount;
        }

        public int getActiveDeletionCount() {
            return activeDeletionCount;
        }

        public int getRotatingSnapshotCount() {
            return rotatingSnapshotCount;
        }

        public int getManualSnapshotCount() {
            return manualSnapshotCount;
        }

        public String getLatestSnapshotName() {
            return latestSnapshotName;
        }

        public String getLastTransactionDescription() {
            return lastTransactionDescription;
        }

        public long getSkippedSuspiciousSnapshots() {
            return skippedSuspiciousSnapshots;
        }
    }
}
