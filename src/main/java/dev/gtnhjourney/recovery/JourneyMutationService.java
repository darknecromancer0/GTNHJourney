package dev.gtnhjourney.recovery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.research.ResearchKey;

/** Server-authoritative facade for explicit Journey mutations and recovery actions. */
public final class JourneyMutationService {

    public boolean deleteExact(EntityPlayerMP player, ResearchKey key, String description) {
        return engine(player).deleteExact(key, description);
    }

    public int undo(EntityPlayerMP player, int count) {
        return engine(player).undo(count);
    }

    public int redo(EntityPlayerMP player, int count) {
        return engine(player).redo(count);
    }

    public int restoreDeleted(EntityPlayerMP player, int count) {
        return engine(player).restoreDeleted(count);
    }

    public void notePassiveMutation(EntityPlayerMP player) {
        if (player != null) engine(player).notePassiveMutation();
    }

    public void notePassiveMutation(EntityPlayerMP player, List<ItemStack> newlyUnlocked) {
        if (player == null) return;
        ResearchMutationEngine engine = engine(player);
        engine.notePassiveMutation();
        if (newlyUnlocked == null) return;
        for (ItemStack stack : newlyUnlocked) {
            if (stack == null || stack.getItem() == null) continue;
            try {
                engine.notePassivePresent(ItemStackKeyFactory.from(stack));
            } catch (IllegalArgumentException ignored) {
                // A malformed presentation endpoint must not break passive research acquisition.
            }
        }
    }

    /**
     * Applies many observed stacks using the same semantic expansion as ordinary research, then records only the states
     * that this bulk operation actually added as one transaction.
     */
    public int applyBulkAdd(EntityPlayerMP player, List<ItemStack> observedStacks, String description) {
        if (player == null || observedStacks == null || observedStacks.isEmpty()) return 0;
        JourneyResearchData research = researchData(player);
        UUID playerId = player.getUniqueID();
        ResearchStateSnapshot before = research.captureState(playerId);
        Set<ResearchKey> beforeKeys = keySet(before);

        for (ItemStack stack : observedStacks) {
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            try {
                research.unlockStates(playerId, stack);
            } catch (RuntimeException ignored) {
                // Broken optional-mod observations must not abort a migration batch.
            } catch (LinkageError ignored) {
                // Optional-mod linkage failures are isolated per candidate.
            }
        }

        ResearchStateSnapshot after = research.captureState(playerId);
        List<ResearchEntrySnapshot> added = new ArrayList<ResearchEntrySnapshot>();
        for (ResearchEntrySnapshot entry : after.entries()) {
            if (!beforeKeys.contains(entry.key())) added.add(entry);
        }
        if (added.isEmpty()) return 0;
        ResearchMutationEngine engine = engine(player);
        for (ResearchEntrySnapshot entry : added) engine.notePassivePresent(entry.key());
        engine.recordApplied(added, java.util.Collections.<ResearchEntrySnapshot>emptyList(), description);
        return added.size();
    }

    public int undoDepth(EntityPlayerMP player) {
        if (player == null) return 0;
        return recoveryData(player).undoDepth(player.getUniqueID());
    }

    public int redoDepth(EntityPlayerMP player) {
        if (player == null) return 0;
        return recoveryData(player).redoDepth(player.getUniqueID());
    }

    public int activeDeletionCount(EntityPlayerMP player) {
        if (player == null) return 0;
        return recoveryData(player).activeDeletionCount(player.getUniqueID());
    }

    public int deletionCount(EntityPlayerMP player) {
        if (player == null) return 0;
        return recoveryData(player).deletionCount(player.getUniqueID());
    }

    private ResearchMutationEngine engine(EntityPlayerMP player) {
        if (player == null) throw new IllegalArgumentException("player must not be null");
        return new ResearchMutationEngine(researchData(player), recoveryData(player), player.getUniqueID());
    }

    private static JourneyResearchData researchData(EntityPlayerMP player) {
        return JourneyResearchData.get(rootWorld(player));
    }

    private static JourneyRecoveryData recoveryData(EntityPlayerMP player) {
        return JourneyRecoveryData.get(rootWorld(player));
    }

    private static World rootWorld(EntityPlayerMP player) {
        World root = DimensionManager.getWorld(0);
        return root == null ? player.worldObj : root;
    }

    private static Set<ResearchKey> keySet(ResearchStateSnapshot snapshot) {
        Set<ResearchKey> keys = new HashSet<ResearchKey>();
        if (snapshot != null) for (ResearchEntrySnapshot entry : snapshot.entries()) keys.add(entry.key());
        return keys;
    }
}
