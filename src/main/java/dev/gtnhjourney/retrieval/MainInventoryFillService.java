package dev.gtnhjourney.retrieval;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** Fills only empty main-inventory slots with independent copies of one exact researched template. */
public final class MainInventoryFillService {

    private MainInventoryFillService() {}

    public static int fillEmptyMainSlots(EntityPlayerMP player, ItemStack template) {
        if (player == null || player.inventory == null) return 0;
        return fillEmptySlots(player.inventory.mainInventory, template, player.inventory.getInventoryStackLimit());
    }

    static int fillEmptySlots(ItemStack[] slots, ItemStack template, int inventoryLimit) {
        if (slots == null || template == null || template.getItem() == null) return 0;
        int amount = Math.max(1, Math.min(Math.max(1, inventoryLimit), template.getMaxStackSize()));
        int filled = 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) continue;
            ItemStack copy = template.copy();
            copy.stackSize = amount;
            slots[i] = copy;
            filled++;
        }
        return filled;
    }
}
