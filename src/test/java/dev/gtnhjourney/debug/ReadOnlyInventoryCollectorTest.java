package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ReadOnlyInventoryCollectorTest {

    @Test
    public void collectsValidSlotsAsCopiesAndIsolatesBrokenSlots() {
        final Item firstItem = new Item();
        final Item lastItem = new Item();
        final ItemStack first = new ItemStack(firstItem, 12, 2);
        final ItemStack last = new ItemStack(lastItem, 3, 7);

        List<ItemStack> result = ReadOnlyInventoryCollector.collect(new ReadOnlyInventoryCollector.SlotSource() {

            @Override
            public int size() {
                return 5;
            }

            @Override
            public ItemStack get(int slot) {
                if (slot == 0) return first;
                if (slot == 1) return null;
                if (slot == 2) throw new IllegalStateException("broken mod slot");
                if (slot == 3) return new ItemStack(new Item(), 0, 0);
                return last;
            }
        });

        assertEquals(2, result.size());
        assertEquals(firstItem, result.get(0).getItem());
        assertEquals(12, result.get(0).stackSize);
        assertEquals(lastItem, result.get(1).getItem());
        assertEquals(3, result.get(1).stackSize);

        result.get(0).stackSize = 1;
        assertEquals(12, first.stackSize);
    }

    @Test
    public void brokenStackCopyDoesNotAbortLaterInventoryCandidates() {
        final Item validItem = new Item();
        final ItemStack broken = new ItemStack(new Item(), 1, 0) {

            @Override
            public ItemStack copy() {
                throw new NoClassDefFoundError("broken optional stack copy");
            }
        };
        final ItemStack valid = new ItemStack(validItem, 4, 3);

        List<ItemStack> result = ReadOnlyInventoryCollector.collect(new ReadOnlyInventoryCollector.SlotSource() {

            @Override
            public int size() {
                return 2;
            }

            @Override
            public ItemStack get(int slot) {
                return slot == 0 ? broken : valid;
            }
        });

        assertEquals(1, result.size());
        assertEquals(validItem, result.get(0).getItem());
        assertEquals(4, result.get(0).stackSize);
    }

    @Test
    public void brokenSizeFailsClosed() {
        List<ItemStack> result = ReadOnlyInventoryCollector.collect(new ReadOnlyInventoryCollector.SlotSource() {

            @Override
            public int size() {
                throw new LinkageError("optional inventory API missing");
            }

            @Override
            public ItemStack get(int slot) {
                return null;
            }
        });

        assertEquals(0, result.size());
    }
}
