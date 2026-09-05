package dev.gtnhjourney.recovery;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import dev.gtnhjourney.minecraft.NbtCanonicalizer;

/** Exact main/armor/Baubles snapshot used only around keepInventory deaths. */
public final class DeathInventorySnapshot {

    private static final String MAIN = "Main";
    private static final String ARMOR = "Armor";
    private static final String BAUBLES = "Baubles";
    private static final String BAUBLE_SLOTS = "BaubleSlots";

    private final NBTTagCompound payload;

    private DeathInventorySnapshot(NBTTagCompound payload) {
        this.payload = copy(payload);
    }

    public static DeathInventorySnapshot capture(EntityPlayerMP player) {
        NBTTagCompound root = new NBTTagCompound();
        if (player == null) return new DeathInventorySnapshot(root);
        root.setTag(MAIN, writeArray(player.inventory.mainInventory));
        root.setTag(ARMOR, writeArray(player.inventory.armorInventory));
        IInventory baubles = OptionalBaublesInventoryAccess.get(player);
        if (baubles != null) {
            root.setInteger(BAUBLE_SLOTS, baubles.getSizeInventory());
            root.setTag(BAUBLES, writeInventory(baubles));
        }
        return new DeathInventorySnapshot(root);
    }

    public static DeathInventorySnapshot fromNbt(NBTTagCompound payload) {
        return new DeathInventorySnapshot(payload);
    }

    public NBTTagCompound toNbt() { return copy(payload); }

    public boolean sameContents(DeathInventorySnapshot other) {
        if (other == null) return false;
        try {
            return NbtCanonicalizer.canonicalize(payload).equals(NbtCanonicalizer.canonicalize(other.payload));
        } catch (RuntimeException ignored) {
            return payload.toString().equals(other.payload.toString());
        }
    }

    /** Counts missing item units, ignoring harmless slot moves within the same inventory domain. */
    public int missingUnitsComparedWith(DeathInventorySnapshot current) {
        if (current == null) return totalUnits(payload);
        return missingUnits(payload.getTagList(MAIN, 10), current.payload.getTagList(MAIN, 10))
            + missingUnits(payload.getTagList(ARMOR, 10), current.payload.getTagList(ARMOR, 10))
            + missingUnits(payload.getTagList(BAUBLES, 10), current.payload.getTagList(BAUBLES, 10));
    }

    /** Non-destructively returns only contents that are missing from the player's current corresponding domains. */
    public int restoreMissing(EntityPlayerMP player) {
        if (player == null) return 0;
        int restored = 0;
        restored += restoreMain(player, payload.getTagList(MAIN, 10));
        restored += restoreArmor(player, payload.getTagList(ARMOR, 10));
        restored += restoreBaubles(player, payload.getTagList(BAUBLES, 10));
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        return restored;
    }

    /** Exact replay used by undo/redo only when current contents still equal the expected source snapshot. */
    public static boolean applyExactIfCurrentMatches(
        EntityPlayerMP player,
        DeathInventorySnapshot expectedCurrent,
        DeathInventorySnapshot target) {
        if (player == null || expectedCurrent == null || target == null) return false;
        if (!capture(player).sameContents(expectedCurrent)) return false;
        applyArray(player.inventory.mainInventory, target.payload.getTagList(MAIN, 10));
        applyArray(player.inventory.armorInventory, target.payload.getTagList(ARMOR, 10));
        IInventory baubles = OptionalBaublesInventoryAccess.get(player);
        if (baubles != null) applyInventory(baubles, target.payload.getTagList(BAUBLES, 10));
        player.inventory.markDirty();
        if (baubles != null) baubles.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        return capture(player).sameContents(target);
    }

    public int totalUnits() { return totalUnits(payload); }

    private static int restoreMain(EntityPlayerMP player, NBTTagList wanted) {
        Map<String, Integer> remaining = counts(writeArray(player.inventory.mainInventory));
        int restored = 0;
        for (int i = 0; i < wanted.tagCount(); i++) {
            NBTTagCompound entry = wanted.getCompoundTagAt(i);
            ItemStack stack = readStack(entry);
            if (stack == null) continue;
            int missing = consumeExisting(remaining, stack);
            if (missing <= 0) continue;
            ItemStack give = stack.copy();
            give.stackSize = missing;
            int before = give.stackSize;
            player.inventory.addItemStackToInventory(give);
            int inserted = before - Math.max(0, give.stackSize);
            restored += inserted;
            if (give.stackSize > 0) {
                restored += give.stackSize;
                player.dropPlayerItemWithRandomChoice(give, false);
            }
        }
        return restored;
    }

    private static int restoreArmor(EntityPlayerMP player, NBTTagList wanted) {
        Map<String, Integer> remaining = counts(writeArray(player.inventory.armorInventory));
        int restored = 0;
        for (int i = 0; i < wanted.tagCount(); i++) {
            NBTTagCompound entry = wanted.getCompoundTagAt(i);
            ItemStack stack = readStack(entry);
            if (stack == null) continue;
            int missing = consumeExisting(remaining, stack);
            if (missing <= 0) continue;
            int slot = entry.getInteger("Slot");
            ItemStack give = stack.copy();
            give.stackSize = missing;
            if (slot >= 0 && slot < player.inventory.armorInventory.length
                && player.inventory.armorInventory[slot] == null && give.stackSize == stack.stackSize) {
                player.inventory.armorInventory[slot] = give;
                restored += give.stackSize;
            } else {
                restored += giveToMainOrDrop(player, give);
            }
        }
        return restored;
    }

    private static int restoreBaubles(EntityPlayerMP player, NBTTagList wanted) {
        IInventory baubles = OptionalBaublesInventoryAccess.get(player);
        if (baubles == null) return 0;
        Map<String, Integer> remaining = counts(writeInventory(baubles));
        int restored = 0;
        for (int i = 0; i < wanted.tagCount(); i++) {
            NBTTagCompound entry = wanted.getCompoundTagAt(i);
            ItemStack stack = readStack(entry);
            if (stack == null) continue;
            int missing = consumeExisting(remaining, stack);
            if (missing <= 0) continue;
            int slot = entry.getInteger("Slot");
            ItemStack give = stack.copy();
            give.stackSize = missing;
            if (slot >= 0 && slot < baubles.getSizeInventory() && baubles.getStackInSlot(slot) == null
                && baubles.isItemValidForSlot(slot, give) && give.stackSize == stack.stackSize) {
                baubles.setInventorySlotContents(slot, give);
                restored += give.stackSize;
            } else {
                restored += giveToMainOrDrop(player, give);
            }
        }
        baubles.markDirty();
        return restored;
    }

    private static int giveToMainOrDrop(EntityPlayerMP player, ItemStack give) {
        if (give == null || give.stackSize <= 0) return 0;
        int before = give.stackSize;
        player.inventory.addItemStackToInventory(give);
        int inserted = before - Math.max(0, give.stackSize);
        if (give.stackSize > 0) {
            inserted += give.stackSize;
            player.dropPlayerItemWithRandomChoice(give, false);
        }
        return inserted;
    }

    /** Returns how many units of this wanted stack are not covered by the mutable current-count map. */
    private static int consumeExisting(Map<String, Integer> remaining, ItemStack wanted) {
        String signature = signature(wanted);
        int available = remaining.containsKey(signature) ? remaining.get(signature).intValue() : 0;
        int consumed = Math.min(available, wanted.stackSize);
        if (consumed > 0) remaining.put(signature, Integer.valueOf(available - consumed));
        return wanted.stackSize - consumed;
    }

    private static int missingUnits(NBTTagList wanted, NBTTagList current) {
        Map<String, Integer> remaining = counts(current);
        int missing = 0;
        for (int i = 0; i < wanted.tagCount(); i++) {
            ItemStack stack = readStack(wanted.getCompoundTagAt(i));
            if (stack != null) missing += consumeExisting(remaining, stack);
        }
        return missing;
    }

    private static Map<String, Integer> counts(NBTTagList list) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = readStack(list.getCompoundTagAt(i));
            if (stack == null) continue;
            String signature = signature(stack);
            Integer old = result.get(signature);
            result.put(signature, Integer.valueOf((old == null ? 0 : old.intValue()) + stack.stackSize));
        }
        return result;
    }

    private static String signature(ItemStack stack) {
        ItemStack one = stack.copy();
        one.stackSize = 1;
        NBTTagCompound tag = new NBTTagCompound();
        one.writeToNBT(tag);
        try {
            return NbtCanonicalizer.canonicalize(tag);
        } catch (RuntimeException ignored) {
            return tag.toString();
        }
    }

    private static NBTTagList writeArray(ItemStack[] stacks) {
        NBTTagList list = new NBTTagList();
        if (stacks == null) return list;
        for (int i = 0; i < stacks.length; i++) append(list, i, stacks[i]);
        return list;
    }

    private static NBTTagList writeInventory(IInventory inventory) {
        NBTTagList list = new NBTTagList();
        if (inventory == null) return list;
        for (int i = 0; i < inventory.getSizeInventory(); i++) append(list, i, inventory.getStackInSlot(i));
        return list;
    }

    private static void append(NBTTagList list, int slot, ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return;
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("Slot", slot);
        NBTTagCompound stackTag = new NBTTagCompound();
        stack.writeToNBT(stackTag);
        entry.setTag("Stack", stackTag);
        list.appendTag(entry);
    }

    private static ItemStack readStack(NBTTagCompound entry) {
        if (entry == null || !entry.hasKey("Stack", 10)) return null;
        ItemStack stack = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("Stack"));
        return stack == null || stack.getItem() == null || stack.stackSize <= 0 ? null : stack;
    }

    private static void applyArray(ItemStack[] target, NBTTagList list) {
        if (target == null) return;
        for (int i = 0; i < target.length; i++) target[i] = null;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getInteger("Slot");
            if (slot >= 0 && slot < target.length) target[slot] = readStack(entry);
        }
    }

    private static void applyInventory(IInventory target, NBTTagList list) {
        if (target == null) return;
        for (int i = 0; i < target.getSizeInventory(); i++) target.setInventorySlotContents(i, null);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getInteger("Slot");
            if (slot >= 0 && slot < target.getSizeInventory()) target.setInventorySlotContents(slot, readStack(entry));
        }
    }

    private static int totalUnits(NBTTagCompound root) {
        return totalUnits(root.getTagList(MAIN, 10)) + totalUnits(root.getTagList(ARMOR, 10))
            + totalUnits(root.getTagList(BAUBLES, 10));
    }

    private static int totalUnits(NBTTagList list) {
        int total = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = readStack(list.getCompoundTagAt(i));
            if (stack != null) total += stack.stackSize;
        }
        return total;
    }

    private static NBTTagCompound copy(NBTTagCompound value) {
        return value == null ? new NBTTagCompound() : (NBTTagCompound) value.copy();
    }
}
