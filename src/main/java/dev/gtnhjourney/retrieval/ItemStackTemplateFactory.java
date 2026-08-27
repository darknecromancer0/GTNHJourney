package dev.gtnhjourney.retrieval;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.gtnhjourney.minecraft.GtToolStatePolicy;
import dev.gtnhjourney.research.ResearchKey;

/** Reconstructs only a server-persisted researched stack template that is still issuable by the live runtime. */
public final class ItemStackTemplateFactory {

    private ItemStackTemplateFactory() {}

    public static ItemStack create(ResearchKey key, NBTTagCompound originalTag, int requestedAmount) {
        if (key == null) return null;
        int colon = key.getItemId()
            .indexOf(':');
        if (colon <= 0 || colon == key.getItemId()
            .length() - 1) return null;
        String modId = key.getItemId()
            .substring(0, colon);
        String name = key.getItemId()
            .substring(colon + 1);
        try {
            Item item = GameRegistry.findItem(modId, name);
            if (item == null) return null;

            ItemStack stack = new ItemStack(item, 1, key.getMeta());
            if (originalTag != null) stack.setTagCompound((NBTTagCompound) originalTag.copy());
            if (GtToolStatePolicy.isKnownInvalidToolState(stack)) return null;
            // Some modded items derive their stack limit from NBT, so clamp only after restoring the exact tag.
            stack.stackSize = RetrievalPolicy.clampAmount(requestedAmount, stack.getMaxStackSize());
            return stack;
        } catch (RuntimeException brokenItem) {
            return null;
        }
    }
}
