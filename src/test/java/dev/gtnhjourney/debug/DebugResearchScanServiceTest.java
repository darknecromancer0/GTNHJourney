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

import dev.gtnhjourney.research.ResearchKey;

public class DebugResearchScanServiceTest {

    @Test
    public void keepsBlockAndContentsModesStrictlySeparatedAndCombinesThemOnlyForArea16() {
        final Item common = new Item();
        final int[] unavailableReads = { 0 };

        DebugResearchScanService service = new DebugResearchScanService(
            new DebugResearchScanService.WorldAdapter() {

                @Override
                public boolean canRead(int x, int y, int z) {
                    return y >= 0 && y < 256 && ((x == 10 && y == 20 && z == 30) || (x == 11 && y == 20 && z == 30));
                }

                @Override
                public ItemStack blockCandidate(int x, int y, int z) {
                    if (!canRead(x, y, z)) {
                        unavailableReads[0]++;
                        throw new AssertionError("unavailable cell was read");
                    }
                    if (x == 10) return new ItemStack(common, 1, 0);
                    return tagged(common, "lava");
                }

                @Override
                public List<ItemStack> inventoryContents(int x, int y, int z) {
                    if (!canRead(x, y, z)) {
                        unavailableReads[0]++;
                        throw new AssertionError("unavailable cell was read");
                    }
                    if (x == 10) return Arrays.asList(new ItemStack(common, 32, 0), tagged(common, "water"));
                    return Collections.emptyList();
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

        DebugResearchScanResult block = service.scanBlock(10, 20, 30);
        assertEquals(0, block.getPositionsVisited());
        assertEquals(1, block.getBlockCandidates());
        assertEquals(0, block.getInventoriesVisited());
        assertEquals(1, block.getRawStacks());
        assertEquals(1, block.getUniqueCandidates());
        assertTrue(!block.getCandidates().get(0).hasTagCompound());

        DebugResearchScanResult contents = service.scanContents(10, 20, 30);
        assertEquals(0, contents.getPositionsVisited());
        assertEquals(0, contents.getBlockCandidates());
        assertEquals(1, contents.getInventoriesVisited());
        assertEquals(2, contents.getRawStacks());
        assertEquals(2, contents.getUniqueCandidates());
        assertTrue(contents.getCandidates().stream().anyMatch(stack -> hasFluid(stack, "water")));

        DebugResearchScanResult area = service.scanArea16(10, 20, 30);
        assertEquals(4096, area.getPositionsVisited());
        assertEquals(2, area.getBlockCandidates());
        assertEquals(2, area.getInventoriesVisited());
        assertEquals(4, area.getRawStacks());
        assertEquals(3, area.getUniqueCandidates());
        assertEquals(0, unavailableReads[0]);
        assertTrue(area.getCandidates().stream().anyMatch(stack -> hasFluid(stack, "water")));
        assertTrue(area.getCandidates().stream().anyMatch(stack -> hasFluid(stack, "lava")));
    }

    @Test
    public void isolatesBrokenModdedReadsInsteadOfAbortingTheAreaScan() {
        final Item common = new Item();
        DebugResearchScanService service = new DebugResearchScanService(
            new DebugResearchScanService.WorldAdapter() {

                @Override
                public boolean canRead(int x, int y, int z) {
                    return (x == 10 || x == 11) && y == 20 && z == 30;
                }

                @Override
                public ItemStack blockCandidate(int x, int y, int z) {
                    if (x == 10) throw new IllegalStateException("broken optional block integration");
                    return new ItemStack(common, 1, 0);
                }

                @Override
                public List<ItemStack> inventoryContents(int x, int y, int z) {
                    if (x == 10) throw new LinkageError("broken optional inventory integration");
                    return Collections.singletonList(tagged(common, "water"));
                }
            },
            stack -> stack.hasTagCompound() ? new ResearchKey("example:tank", 0, "fluid=water")
                : new ResearchKey("example:block", 0, ""));

        DebugResearchScanResult area = service.scanArea16(10, 20, 30);

        assertEquals(4096, area.getPositionsVisited());
        assertEquals(1, area.getBlockCandidates());
        assertEquals(1, area.getInventoriesVisited());
        assertEquals(2, area.getRawStacks());
        assertEquals(2, area.getUniqueCandidates());
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
