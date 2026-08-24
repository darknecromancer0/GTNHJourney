package dev.gtnhjourney.acquisition;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** Enumerates only inventory surfaces that are proven to belong to the player, never arbitrary open GUI slots. */
public final class PlayerInventoryScanner {

    public interface StackVisitor {

        void visit(ItemStack stack);
    }

    public interface SlotVisitor {

        void visit(String slotId, ItemStack stack);
    }

    private PlayerInventoryScanner() {}

    public static void scan(EntityPlayerMP player, final StackVisitor visitor) {
        if (player == null || visitor == null) return;
        scanSlots(player, new SlotVisitor() {

            @Override
            public void visit(String slotId, ItemStack stack) {
                visitNonEmpty(visitor, stack);
            }
        });
    }

    /** Visits only non-empty real player-owned slots, but supplies a stable slot id for change caching. */
    public static void scanSlots(EntityPlayerMP player, final SlotVisitor visitor) {
        if (player == null || visitor == null) return;
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            visitSlot(visitor, "main:" + i, player.inventory.mainInventory[i]);
        }
        for (int i = 0; i < player.inventory.armorInventory.length; i++) {
            visitSlot(visitor, "armor:" + i, player.inventory.armorInventory[i]);
        }
        visitSlot(visitor, "cursor", player.inventory.getItemStack());
        OptionalPlayerInventories.scanSlots(player, new OptionalPlayerInventories.SlotStackConsumer() {

            @Override
            public void accept(String slotId, ItemStack stack) {
                visitSlot(visitor, slotId, stack);
            }
        });
    }

    private static void visitSlot(SlotVisitor visitor, String slotId, ItemStack stack) {
        if (stack != null && stack.getItem() != null && stack.stackSize > 0) visitor.visit(slotId, stack);
    }

    private static void visitNonEmpty(StackVisitor visitor, ItemStack stack) {
        if (stack != null && stack.getItem() != null && stack.stackSize > 0) visitor.visit(stack);
    }
}
