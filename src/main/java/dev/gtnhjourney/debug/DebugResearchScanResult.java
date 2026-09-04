package dev.gtnhjourney.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/** Immutable result and diagnostics for one debug research scan. */
public final class DebugResearchScanResult {

    private final List<ItemStack> candidates;
    private final int positionsVisited;
    private final int blockCandidates;
    private final int inventoriesVisited;
    private final int droppedItemsVisited;
    private final int rawStacks;
    private final int newlyUnlockedStates;

    public DebugResearchScanResult(
        List<ItemStack> candidates,
        int positionsVisited,
        int blockCandidates,
        int inventoriesVisited,
        int rawStacks,
        int newlyUnlockedStates) {
        this(candidates, positionsVisited, blockCandidates, inventoriesVisited, 0, rawStacks, newlyUnlockedStates);
    }

    public DebugResearchScanResult(
        List<ItemStack> candidates,
        int positionsVisited,
        int blockCandidates,
        int inventoriesVisited,
        int droppedItemsVisited,
        int rawStacks,
        int newlyUnlockedStates) {
        this.candidates = copyCandidates(candidates);
        this.positionsVisited = positionsVisited;
        this.blockCandidates = blockCandidates;
        this.inventoriesVisited = inventoriesVisited;
        this.droppedItemsVisited = droppedItemsVisited;
        this.rawStacks = rawStacks;
        this.newlyUnlockedStates = newlyUnlockedStates;
    }

    public List<ItemStack> getCandidates() {
        return copyCandidates(candidates);
    }

    public int getPositionsVisited() {
        return positionsVisited;
    }

    public int getBlockCandidates() {
        return blockCandidates;
    }

    public int getInventoriesVisited() {
        return inventoriesVisited;
    }

    public int getDroppedItemsVisited() {
        return droppedItemsVisited;
    }

    public int getRawStacks() {
        return rawStacks;
    }

    public int getUniqueCandidates() {
        return candidates.size();
    }

    public int getNewlyUnlockedStates() {
        return newlyUnlockedStates;
    }

    public DebugResearchScanResult withNewlyUnlockedStates(int value) {
        return new DebugResearchScanResult(
            candidates,
            positionsVisited,
            blockCandidates,
            inventoriesVisited,
            droppedItemsVisited,
            rawStacks,
            value);
    }

    private static List<ItemStack> copyCandidates(List<ItemStack> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();

        List<ItemStack> copy = new ArrayList<ItemStack>(source.size());
        for (ItemStack stack : source) {
            if (stack != null) copy.add(stack.copy());
        }
        return Collections.unmodifiableList(copy);
    }
}
