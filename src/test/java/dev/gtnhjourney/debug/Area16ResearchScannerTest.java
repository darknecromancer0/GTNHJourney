package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.debug.Area16Planner.Position;
import dev.gtnhjourney.research.ResearchKey;

public class Area16ResearchScannerTest {

    @Test
    public void scansExactAreaWithoutReadingUnavailableCellsAndDeduplicatesBlocksAndContents() {
        final Item common = new Item();
        final Position first = new Position(10, 20, 30);
        final Position second = new Position(11, 20, 30);
        final Position broken = new Position(12, 20, 30);
        final int[] blockReads = { 0 };
        final int[] inventoryReads = { 0 };

        DebugResearchScanResult result = Area16ResearchScanner.scan(
            10,
            20,
            30,
            new Area16ResearchScanner.CellReader() {

                @Override
                public boolean canRead(Position position) {
                    return position.equals(first) || position.equals(second) || position.equals(broken);
                }

                @Override
                public ItemStack block(Position position) {
                    blockReads[0]++;
                    if (position.equals(broken)) throw new IllegalStateException("broken optional block integration");
                    if (position.equals(first)) return new ItemStack(common, 1, 0);
                    if (position.equals(second)) return tagged(common, "lava");
                    return null;
                }

                @Override
                public List<ItemStack> inventoryContents(Position position) {
                    inventoryReads[0]++;
                    if (position.equals(first)) {
                        return Arrays.asList(new ItemStack(common, 32, 0), tagged(common, "water"));
                    }
                    if (position.equals(second)) return Collections.emptyList();
                    if (position.equals(broken)) throw new LinkageError("broken optional inventory integration");
                    return null;
                }
            },
            new ResearchCandidateDeduplicator.IdentityResolver() {

                @Override
                public ResearchKey identity(ItemStack stack) {
                    if (!stack.hasTagCompound()) return new ResearchKey("example:block", 0, "");
                    String fluid = stack.getTagCompound().getString("fluid");
                    return new ResearchKey("example:tank", 0, "fluid=" + fluid);
                }
            });

        assertEquals(4096, result.getPositionsVisited());
        assertEquals(2, result.getBlockCandidates());
        assertEquals(2, result.getInventoriesVisited());
        assertEquals(4, result.getRawStacks());
        assertEquals(3, result.getUniqueCandidates());
        assertEquals(3, blockReads[0]);
        assertEquals(3, inventoryReads[0]);
        assertTrue(result.getCandidates().stream().anyMatch(stack -> !stack.hasTagCompound()));
        assertTrue(result.getCandidates().stream().anyMatch(stack -> hasFluid(stack, "water")));
        assertTrue(result.getCandidates().stream().anyMatch(stack -> hasFluid(stack, "lava")));
    }

    private static ItemStack tagged(Item item, String fluid) {
        ItemStack stack = new ItemStack(item, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("fluid", fluid);
        stack.setTagCompound(tag);
        return stack;
    }

    private static boolean hasFluid(ItemStack stack, String fluid) {
        return stack != null && stack.hasTagCompound() && fluid.equals(stack.getTagCompound().getString("fluid"));
    }
}
