package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import codechicken.nei.LayoutManager;
import codechicken.nei.api.API;
import codechicken.nei.api.ItemInfo;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.ItemStack;

/**
 * Compatibility boundary for the one NEI-internal operation the public API does not expose: removing
 * ItemStack variants previously injected by Journey. All other integration stays on NEI's public API.
 */
final class JourneyNEIVariantBridge {
    private static final List<ItemStack> ownedVariants = new ArrayList<ItemStack>();
    private static final Set<ResearchKey> ownedKeys = new LinkedHashSet<ResearchKey>();

    private JourneyNEIVariantBridge() {}

    /** Replaces Journey-owned exact variants. Returns true only when NEI's item universe actually changed. */
    static boolean replaceWith(List<ItemStack> exactResearchStacks) {
        Set<ResearchKey> requestedKeys = keysOf(exactResearchStacks);
        if (requestedKeys.equals(ownedKeys)) return false;

        removeOwnedVariantsInternal();
        if (exactResearchStacks != null) {
            synchronized (ItemInfo.itemVariants) {
                for (ItemStack original : exactResearchStacks) {
                    if (original == null || original.getItem() == null) continue;
                    ResearchKey key = safeKey(original);
                    if (key == null || !requestedKeys.contains(key)) continue;
                    ItemStack variant = original.copy();
                    variant.stackSize = 1;
                    // Do not claim ownership of an entry somebody else already registered.
                    if (ItemInfo.itemVariants.containsEntry(variant.getItem(), variant)) continue;
                    API.addItemVariant(variant.getItem(), variant);
                    ownedVariants.add(variant);
                }
            }
        }
        ownedKeys.clear();
        ownedKeys.addAll(requestedKeys);
        LayoutManager.markItemsDirty();
        return true;
    }

    /** Clears Journey-owned variants. Returns true only when something was removed. */
    static boolean clear() {
        if (ownedVariants.isEmpty() && ownedKeys.isEmpty()) return false;
        removeOwnedVariantsInternal();
        ownedKeys.clear();
        LayoutManager.markItemsDirty();
        return true;
    }

    private static void removeOwnedVariantsInternal() {
        if (ownedVariants.isEmpty()) return;
        synchronized (ItemInfo.itemVariants) {
            for (ItemStack variant : ownedVariants) {
                try {
                    ItemInfo.itemVariants.remove(variant.getItem(), variant);
                } catch (RuntimeException ignored) {}
            }
        }
        ownedVariants.clear();
    }

    private static Set<ResearchKey> keysOf(List<ItemStack> stacks) {
        Set<ResearchKey> out = new LinkedHashSet<ResearchKey>();
        if (stacks == null) return out;
        for (ItemStack stack : stacks) {
            ResearchKey key = safeKey(stack);
            if (key != null) out.add(key);
        }
        return out;
    }

    private static ResearchKey safeKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            return ItemStackKeyFactory.from(stack);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
