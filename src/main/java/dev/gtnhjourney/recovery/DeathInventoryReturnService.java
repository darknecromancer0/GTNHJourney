package dev.gtnhjourney.recovery;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.persistence.DeathInventoryRecoveryData;

/** Manual, non-destructive return path for a detected keepInventory mismatch. */
public final class DeathInventoryReturnService {

    private final JourneyReversibleActionService actions;

    public DeathInventoryReturnService(JourneyReversibleActionService actions) {
        if (actions == null) throw new IllegalArgumentException("actions must not be null");
        this.actions = actions;
    }

    public Result status(EntityPlayerMP player) {
        DeathInventoryRecoveryData.Record record = record(player);
        if (record == null || record.pre() == null) return new Result(false, 0, 0);
        DeathInventorySnapshot current = DeathInventorySnapshot.capture(player);
        int missing = record.pre().missingUnitsComparedWith(current);
        return new Result(record.mismatch() || missing > 0, missing, 0);
    }

    public Result restore(EntityPlayerMP player) {
        DeathInventoryRecoveryData.Record record = record(player);
        if (player == null || record == null || record.pre() == null) return new Result(false, 0, 0);
        DeathInventorySnapshot before = DeathInventorySnapshot.capture(player);
        int missing = record.pre().missingUnitsComparedWith(before);
        if (missing <= 0) return new Result(false, 0, 0);
        int restored = record.pre().restoreMissing(player);
        DeathInventorySnapshot after = DeathInventorySnapshot.capture(player);
        if (restored > 0 && !before.sameContents(after)) {
            actions.record(
                player,
                JourneyActionKind.DEATH_INVENTORY_RETURN,
                "Return death inventory",
                before.toNbt(),
                after.toNbt());
        }
        return new Result(restored > 0, missing, restored);
    }

    private static DeathInventoryRecoveryData.Record record(EntityPlayerMP player) {
        if (player == null) return null;
        World root = DimensionManager.getWorld(0);
        return DeathInventoryRecoveryData.get(root == null ? player.worldObj : root).record(player.getUniqueID());
    }

    public static final class Result {
        private final boolean recoverable;
        private final int missing;
        private final int restored;

        Result(boolean recoverable, int missing, int restored) {
            this.recoverable = recoverable;
            this.missing = Math.max(0, missing);
            this.restored = Math.max(0, restored);
        }

        public boolean recoverable() { return recoverable; }
        public int missing() { return missing; }
        public int restored() { return restored; }
    }
}
