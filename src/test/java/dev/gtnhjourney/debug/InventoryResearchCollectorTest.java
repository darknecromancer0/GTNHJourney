package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class InventoryResearchCollectorTest {

    @Test
    public void collectsIInventoryStacksAsDefensiveCopiesAndSkipsBrokenSlots() {
        final Item firstItem = new Item();
        final Item lastItem = new Item();
        final ItemStack first = new ItemStack(firstItem, 9, 2);
        final ItemStack last = new ItemStack(lastItem, 4, 7);

        InventoryBasic inventory = new InventoryBasic("debug", false, 4) {

            @Override
            public ItemStack getStackInSlot(int slot) {
                if (slot == 1) throw new IllegalStateException("broken mod slot");
                return super.getStackInSlot(slot);
            }
        };
        inventory.setInventorySlotContents(0, first);
        inventory.setInventorySlotContents(2, null);
        inventory.setInventorySlotContents(3, last);

        List<ItemStack> result = InventoryResearchCollector.collect(inventory);

        assertEquals(2, result.size());
        assertEquals(firstItem, result.get(0).getItem());
        assertEquals(lastItem, result.get(1).getItem());
        result.get(0).stackSize = 1;
        assertEquals(9, first.stackSize);
    }
}
