package dev.gtnhjourney.persistence;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.recovery.ResearchEntrySnapshot;
import dev.gtnhjourney.recovery.ResearchStateSnapshot;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;
import dev.gtnhjourney.research.ResearchRegistry;

/** Facade used by Forge events, commands and network handlers. */
public final class PlayerResearchService {

    public ResearchRegistry registry(EntityPlayerMP player) {
        return data(player).registry(player.getUniqueID());
    }

    public boolean unlock(EntityPlayerMP player, ItemStack stack) {
        return !unlockStates(player, stack).isEmpty();
    }

    public List<ItemStack> unlockStates(EntityPlayerMP player, ItemStack stack) {
        if (player == null || stack == null || stack.getItem() == null) return Collections.emptyList();
        try {
            List<ItemStack> added = data(player).unlockStates(player.getUniqueID(), stack);
            if (!added.isEmpty()) {
                JourneyActivityData activity = activityData(player);
                for (ItemStack unlocked : added) {
                    try {
                        activity.recordUnlock(player.getUniqueID(), ItemStackKeyFactory.from(unlocked));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            return added;
        } catch (RuntimeException failure) {
            recordObservationFailure(stack, failure);
            return Collections.emptyList();
        } catch (LinkageError failure) {
            recordObservationFailure(stack, failure);
            return Collections.emptyList();
        }
    }

    public List<ResearchKey> snapshot(EntityPlayerMP player) {
        return data(player).snapshot(player.getUniqueID());
    }

    public ResearchStateSnapshot captureState(EntityPlayerMP player) {
        return data(player).captureState(player.getUniqueID());
    }

    public ResearchEntrySnapshot removeEntry(EntityPlayerMP player, ResearchKey key) {
        return data(player).removeEntry(player.getUniqueID(), key);
    }

    public boolean restoreEntry(EntityPlayerMP player, ResearchEntrySnapshot entry) {
        return data(player).restoreEntry(player.getUniqueID(), entry);
    }

    public List<ItemStack> snapshotStacks(EntityPlayerMP player) {
        return data(player).snapshotStacks(player.getUniqueID());
    }

    public List<ItemStack> snapshotStacksInUnlockOrder(EntityPlayerMP player) {
        return data(player).snapshotStacksInUnlockOrder(player.getUniqueID());
    }

    public List<ResearchKey> snapshotActivityOrder(EntityPlayerMP player) {
        JourneyResearchData research = data(player);
        return activityData(player)
            .snapshotReconciled(player.getUniqueID(), research.snapshotInUnlockOrder(player.getUniqueID()));
    }

    public List<ResearchKey> snapshotNewest(EntityPlayerMP player, int limit) {
        return data(player).snapshotNewest(player.getUniqueID(), limit);
    }

    public List<ItemStack> snapshotNewestStacks(EntityPlayerMP player, int limit) {
        return data(player).snapshotNewestStacks(player.getUniqueID(), limit);
    }

    public ItemStack retrieve(EntityPlayerMP player, ResearchKey key, int requestedAmount) {
        JourneyResearchData data = data(player);
        if (!data.registry(player.getUniqueID())
            .contains(key)) return null;
        return dev.gtnhjourney.retrieval.ItemStackTemplateFactory
            .create(key, data.template(player.getUniqueID(), key), requestedAmount);
    }

    public ItemStack retrieve(EntityPlayerMP player, ResearchFingerprint fingerprint, int requestedAmount) {
        JourneyResearchData data = data(player);
        ResearchKey key = data.registry(player.getUniqueID())
            .find(fingerprint);
        if (key == null) return null;
        return dev.gtnhjourney.retrieval.ItemStackTemplateFactory
            .create(key, data.template(player.getUniqueID(), key), requestedAmount);
    }

    public ResearchKey resolve(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        if (player == null || fingerprint == null) return null;
        return data(player).registry(player.getUniqueID())
            .find(fingerprint);
    }

    public void recordRetrieval(EntityPlayerMP player, ResearchKey key) {
        if (player == null || key == null) return;
        if (!data(player).registry(player.getUniqueID())
            .contains(key)) return;
        activityData(player).recordRetrieval(player.getUniqueID(), key);
    }

    public boolean forget(EntityPlayerMP player, ResearchKey key) {
        return data(player).forget(player.getUniqueID(), key);
    }

    public int clear(EntityPlayerMP player) {
        return data(player).clear(player.getUniqueID());
    }

    public int undo(EntityPlayerMP player) {
        return data(player).undo(player.getUniqueID());
    }

    public int undoSize(EntityPlayerMP player) {
        return data(player).undoSize(player.getUniqueID());
    }

    public int pruneUnavailable(EntityPlayerMP player) {
        return data(player).pruneUnavailable(player.getUniqueID());
    }

    private static JourneyResearchData data(EntityPlayerMP player) {
        return JourneyResearchData.get(rootWorld(player));
    }

    private static JourneyActivityData activityData(EntityPlayerMP player) {
        return JourneyActivityData.get(rootWorld(player));
    }

    private static World rootWorld(EntityPlayerMP player) {
        if (player == null) throw new IllegalArgumentException("player must not be null");
        World rootWorld = DimensionManager.getWorld(0);
        return rootWorld == null ? player.worldObj : rootWorld;
    }

    private static void recordObservationFailure(ItemStack stack, Throwable failure) {
        String item = stack == null || stack.getItem() == null ? "<null>"
            : stack.getItem()
                .getClass()
                .getName();
        String message = failure == null ? "<unknown>"
            : failure.getClass()
                .getName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
        if (dev.gtnhjourney.diagnostics.ResearchFailureLog.record(item, message)) {
            cpw.mods.fml.common.FMLLog
                .warning("[GTNH Journey] Skipping broken research observation for %s: %s", item, message);
        }
    }
}
