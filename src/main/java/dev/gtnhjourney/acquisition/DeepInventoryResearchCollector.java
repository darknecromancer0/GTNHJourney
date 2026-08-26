package dev.gtnhjourney.acquisition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.minecraft.EmbeddedInventoryPolicy;

/** Manual-only deep scan of real player-owned slots plus bounded embedded/external container contents. */
public final class DeepInventoryResearchCollector {

    public static final int MAX_EMBEDDED_DEPTH = 8;
    public static final int MAX_TOTAL_EMBEDDED_STACKS = 4096;

    private DeepInventoryResearchCollector() {}

    public static Result collect(EntityPlayerMP player) {
        if (player == null) return new Result(Collections.<ItemStack>emptyList(), 0, 0, 0);
        final List<ItemStack> candidates = new ArrayList<ItemStack>();
        final int[] topLevel = new int[] { 0 };
        final int[] embedded = new int[] { 0 };
        final int[] skipped = new int[] { 0 };
        final Set<String> visitedExternalBackpacks = new HashSet<String>();

        PlayerInventoryScanner.scan(player, new PlayerInventoryScanner.StackVisitor() {

            @Override
            public void visit(ItemStack stack) {
                if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return;
                try {
                    candidates.add(stack.copy());
                    topLevel[0]++;
                } catch (RuntimeException | LinkageError failure) {
                    skipped[0]++;
                    return;
                }
                collectNestedSources(stack, 0, candidates, embedded, skipped, visitedExternalBackpacks);
            }
        });
        return new Result(candidates, topLevel[0], embedded[0], skipped[0]);
    }

    private static void collectNestedSources(
        ItemStack owner,
        int depth,
        List<ItemStack> candidates,
        int[] embedded,
        int[] skipped,
        Set<String> visitedExternalBackpacks) {
        if (owner == null || depth > MAX_EMBEDDED_DEPTH || embedded[0] >= MAX_TOTAL_EMBEDDED_STACKS) return;

        // Normal NBT-embedded inventories. embeddedItemTags already walks descendants, so only external sources of the
        // discovered items need another recursion here; rescanning their regular NBT would duplicate descendants.
        if (owner.hasTagCompound()) {
            int remaining = MAX_TOTAL_EMBEDDED_STACKS - embedded[0];
            List<NBTTagCompound> tags;
            try {
                tags = EmbeddedInventoryPolicy.embeddedItemTags(
                    owner.getTagCompound(),
                    Math.max(0, MAX_EMBEDDED_DEPTH - depth),
                    remaining);
            } catch (RuntimeException | LinkageError failure) {
                skipped[0]++;
                tags = Collections.emptyList();
            }
            for (NBTTagCompound serialized : tags) {
                if (embedded[0] >= MAX_TOTAL_EMBEDDED_STACKS) break;
                ItemStack nested = loadSerialized(serialized, skipped);
                if (nested == null) continue;
                addEmbeddedCandidate(nested, candidates, embedded, skipped);
                collectExternalBackpack(
                    nested,
                    depth + 1,
                    candidates,
                    embedded,
                    skipped,
                    visitedExternalBackpacks);
            }
        }

        collectExternalBackpack(owner, depth, candidates, embedded, skipped, visitedExternalBackpacks);
    }

    private static void collectExternalBackpack(
        ItemStack owner,
        int depth,
        List<ItemStack> candidates,
        int[] embedded,
        int[] skipped,
        Set<String> visitedExternalBackpacks) {
        if (depth > MAX_EMBEDDED_DEPTH || embedded[0] >= MAX_TOTAL_EMBEDDED_STACKS) return;
        String externalId = BackpackExternalInventoryReader.externalInstanceId(owner);
        if (externalId == null || !visitedExternalBackpacks.add(externalId)) return;

        int remaining = MAX_TOTAL_EMBEDDED_STACKS - embedded[0];
        List<NBTTagCompound> serialized;
        try {
            serialized = BackpackExternalInventoryReader.serializedStacks(owner, remaining);
        } catch (RuntimeException | LinkageError failure) {
            skipped[0]++;
            return;
        }
        for (NBTTagCompound tag : serialized) {
            if (embedded[0] >= MAX_TOTAL_EMBEDDED_STACKS) break;
            ItemStack nested = loadSerialized(tag, skipped);
            if (nested == null) continue;
            if (!addEmbeddedCandidate(nested, candidates, embedded, skipped)) continue;
            // External contents were not reachable through the owner's ItemStack NBT, so their own regular embedded
            // inventories and any nested external backpacks still need to be traversed.
            collectNestedSources(
                nested,
                depth + 1,
                candidates,
                embedded,
                skipped,
                visitedExternalBackpacks);
        }
    }

    private static ItemStack loadSerialized(NBTTagCompound serialized, int[] skipped) {
        try {
            ItemStack nested = ItemStack.loadItemStackFromNBT(serialized);
            if (nested == null || nested.getItem() == null || nested.stackSize <= 0) {
                skipped[0]++;
                return null;
            }
            return nested;
        } catch (RuntimeException | LinkageError failure) {
            skipped[0]++;
            return null;
        }
    }

    private static boolean addEmbeddedCandidate(
        ItemStack nested,
        List<ItemStack> candidates,
        int[] embedded,
        int[] skipped) {
        try {
            candidates.add(nested.copy());
            embedded[0]++;
            return true;
        } catch (RuntimeException | LinkageError failure) {
            skipped[0]++;
            return false;
        }
    }

    public static final class Result {

        private final List<ItemStack> candidates;
        private final int topLevelStacks;
        private final int embeddedStacks;
        private final int skippedStacks;

        Result(List<ItemStack> candidates, int topLevelStacks, int embeddedStacks, int skippedStacks) {
            List<ItemStack> copies = new ArrayList<ItemStack>();
            if (candidates != null) {
                for (ItemStack stack : candidates) if (stack != null) copies.add(stack.copy());
            }
            this.candidates = Collections.unmodifiableList(copies);
            this.topLevelStacks = Math.max(0, topLevelStacks);
            this.embeddedStacks = Math.max(0, embeddedStacks);
            this.skippedStacks = Math.max(0, skippedStacks);
        }

        public List<ItemStack> candidates() {
            List<ItemStack> copies = new ArrayList<ItemStack>(candidates.size());
            for (ItemStack stack : candidates) copies.add(stack.copy());
            return Collections.unmodifiableList(copies);
        }

        public int topLevelStacks() {
            return topLevelStacks;
        }

        public int embeddedStacks() {
            return embeddedStacks;
        }

        public int skippedStacks() {
            return skippedStacks;
        }
    }
}
