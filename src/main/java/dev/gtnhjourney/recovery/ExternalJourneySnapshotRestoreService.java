package dev.gtnhjourney.recovery;

import java.io.File;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.persistence.JourneySnapshotData;

/** Transactional return path for recovery snapshots kept outside the Minecraft world save. */
public final class ExternalJourneySnapshotRestoreService {

    public Result restoreLatest(EntityPlayerMP player) {
        if (player == null) return new Result(Status.INVALID, null, 0);
        World root = rootWorld(player);
        File worldDirectory = DimensionManager.getCurrentSaveRootDirectory();
        File instanceRoot = ExternalJourneySnapshotArchive.instanceRootFor(worldDirectory);
        String worldName = root.getWorldInfo() == null
            ? (worldDirectory == null ? "world" : worldDirectory.getName())
            : root.getWorldInfo().getWorldName();

        ExternalJourneySnapshotArchive.ArchivedSnapshot archived =
            ExternalJourneySnapshotArchive.latest(instanceRoot, worldName, player.getUniqueID());
        if (archived == null) return new Result(Status.NOT_FOUND, null, 0);
        ResearchStateSnapshot target = archived.state();
        if (!JourneyMutationService.isRestorableState(target)) return new Result(Status.INVALID, archived, 0);

        UUID playerId = player.getUniqueID();
        JourneyResearchData research = JourneyResearchData.get(root);
        ResearchStateSnapshot before = research.captureState(playerId);
        if (ResearchMutationEngine.sameState(before, target)) {
            return new Result(Status.ALREADY_CURRENT, archived, target.size());
        }

        JourneyRecoveryData recovery = JourneyRecoveryData.get(root);
        List<DeletionStateChange> deletionChanges =
            JourneyMutationService.deletionChangesForTarget(recovery, playerId, target);
        JourneySnapshotService snapshots = new JourneySnapshotService(JourneySnapshotData.get(root));
        snapshots.createSafety(
            playerId,
            "before-external-latest-return-" + archived.createdAtMillis(),
            root.getTotalWorldTime(),
            before);

        new ResearchMutationEngine(research, recovery, playerId, RuntimeRecoveryRestorePolicy.INSTANCE)
            .replaceState(target, "Return latest external Journey snapshot", deletionChanges);
        if (!ResearchMutationEngine.sameState(research.captureState(playerId), target)) {
            return new Result(Status.FAILED, archived, 0);
        }
        return new Result(Status.RESTORED, archived, target.size());
    }

    private static World rootWorld(EntityPlayerMP player) {
        World root = DimensionManager.getWorld(0);
        return root == null ? player.worldObj : root;
    }

    public enum Status {
        RESTORED,
        ALREADY_CURRENT,
        NOT_FOUND,
        INVALID,
        FAILED
    }

    public static final class Result {
        private final Status status;
        private final ExternalJourneySnapshotArchive.ArchivedSnapshot snapshot;
        private final int entries;

        Result(Status status, ExternalJourneySnapshotArchive.ArchivedSnapshot snapshot, int entries) {
            this.status = status;
            this.snapshot = snapshot;
            this.entries = entries;
        }

        public Status status() { return status; }
        public ExternalJourneySnapshotArchive.ArchivedSnapshot snapshot() { return snapshot; }
        public int entries() { return entries; }
    }
}
