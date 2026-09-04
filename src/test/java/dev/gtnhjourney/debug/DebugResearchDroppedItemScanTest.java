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

public class DebugResearchDroppedItemScanTest {

    @Test
    public void area16IncludesDroppedStacksOnceAndPreservesTheirNbt() {
        final Item item = new Item();
        final int[] droppedQueries = { 0 };
        final ItemStack tagged = new ItemStack(item, 7, 4);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("variant", "silver");
        tagged.setTagCompound(tag);

        DebugResearchScanService service = new DebugResearchScanService(
            new DebugResearchScanService.WorldAdapter() {

                @Override
                public boolean canRead(int x, int y, int z) {
                    return false;
                }

                @Override
                public ItemStack blockCandidate(int x, int y, int z) {
                    return null;
                }

                @Override
                public List<ItemStack> inventoryContents(int x, int y, int z) {
                    return Collections.emptyList();
                }

                @Override
                public List<ItemStack> droppedItemsInArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
                    droppedQueries[0]++;
                    assertEquals(-6, minX);
                    assertEquals(4, minY);
                    assertEquals(14, minZ);
                    assertEquals(26, maxX);
                    assertEquals(36, maxY);
                    assertEquals(46, maxZ);
                    return Arrays.asList(tagged, tagged.copy());
                }
            },
            stack -> new ResearchKey(
                "example:dropped",
                stack.getItemDamage(),
                stack.hasTagCompound() ? stack.getTagCompound().getString("variant") : ""));

        DebugResearchScanResult result = service.scanArea16(10, 20, 30);

        assertEquals(1, droppedQueries[0]);
        assertEquals(2, result.getDroppedItemsVisited());
        assertEquals(2, result.getRawStacks());
        assertEquals(1, result.getUniqueCandidates());
        assertEquals(1, result.getCandidates().size());
        assertEquals(4, result.getCandidates().get(0).getItemDamage());
        assertEquals("silver", result.getCandidates().get(0).getTagCompound().getString("variant"));
        assertTrue(result.getCandidates().get(0) != tagged);
    }
}
