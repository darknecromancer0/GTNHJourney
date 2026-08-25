package dev.gtnhjourney.debug;

import java.util.Collections;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/** Read-only IInventory adapter for migration research scans. */
public final class InventoryResearchCollector {

    private InventoryResearchCollector() {}

    public static List<ItemStack> collect(final IInventory inventory) {
        if (inventory == null) return Collections.emptyList();
        return ReadOnlyInventoryCollector.collect(new ReadOnlyInventoryCollector.SlotSource() {

            @Override
            public int size() {
                return inventory.getSizeInventory();
            }

            @Override
            public ItemStack get(int slot) {
                return inventory.getStackInSlot(slot);
            }
        });
    }
}
