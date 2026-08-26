package dev.gtnhjourney.acquisition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.minecraft.EmbeddedInventoryPolicy;

/** Manual-only deep scan of real player-owned slots plus structurally proven embedded ItemStack lists. */
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

                if (!stack.hasTagCompound() || embedded[0] >= MAX_TOTAL_EMBEDDED_STACKS) return;
                int remaining = MAX_TOTAL_EMBEDDED_STACKS - embedded[0];
                List<NBTTagCompound> tags;
                try {
                    tags = EmbeddedInventoryPolicy
                        .embeddedItemTags(stack.getTagCompound(), MAX_EMBEDDED_DEPTH, remaining);
                } catch (RuntimeException | LinkageError failure) {
                    skipped[0]++;
                    return;
                }
                for (NBTTagCompound serialized : tags) {
                    if (embedded[0] >= MAX_TOTAL_EMBEDDED_STACKS) break;
                    try {
                        ItemStack nested = ItemStack.loadItemStackFromNBT(serialized);
                        if (nested == null || nested.getItem() == null || nested.stackSize <= 0) {
                            skipped[0]++;
                            continue;
                        }
                        candidates.add(nested.copy());
                        embedded[0]++;
                    } catch (RuntimeException | LinkageError failure) {
                        skipped[0]++;
                    }
                }
            }
        });
        return new Result(candidates, topLevel[0], embedded[0], skipped[0]);
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
