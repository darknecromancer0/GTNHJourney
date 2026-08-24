package dev.gtnhjourney.persistence;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

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
            return data(player).unlockStates(player.getUniqueID(), stack);
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

    public List<ItemStack> snapshotStacks(EntityPlayerMP player) {
        return data(player).snapshotStacks(player.getUniqueID());
    }

    public List<ItemStack> snapshotStacksInUnlockOrder(EntityPlayerMP player) {
        return data(player).snapshotStacksInUnlockOrder(player.getUniqueID());
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
        if (player == null) throw new IllegalArgumentException("player must not be null");
        // WorldSavedData attached to the overworld gives one research database for the entire save.
        World rootWorld = DimensionManager.getWorld(0);
        if (rootWorld == null) rootWorld = player.worldObj;
        return JourneyResearchData.get(rootWorld);
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
