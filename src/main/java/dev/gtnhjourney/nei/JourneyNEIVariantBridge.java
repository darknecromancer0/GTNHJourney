package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import codechicken.nei.ItemList;
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

        List<ItemStack> previousOwned = new ArrayList<ItemStack>(ownedVariants);
        removeOwnedVariantsInternal();
        if (exactResearchStacks != null) {
            synchronized (ItemInfo.itemVariants) {
                for (ItemStack original : exactResearchStacks) {
                    if (original == null || original.getItem() == null) continue;
                    ResearchKey key = authoritativeKey(original);
                    if (key == null || !requestedKeys.contains(key)) continue;
                    boolean nativeExactPresent = nativeExactPresent(original, key, previousOwned);
                    if (!JourneyVariantScope.shouldInjectVariant(key, nativeExactPresent)) continue;

                    ItemStack variant = JourneyPresentationSafety.forNei(original);
                    if (variant == null || variant.getItem() == null) continue;
                    ResearchKey presentationKey = authoritativeKey(variant);
                    boolean remapped = presentationKey == null || !key.equals(presentationKey);
                    if (remapped) JourneyPresentationKeyResolver.register(variant, key);

                    // Do not claim ownership of an entry somebody else already registered.
                    if (ItemInfo.itemVariants.containsEntry(variant.getItem(), variant)) {
                        if (remapped) JourneyPresentationKeyResolver.unregister(variant);
                        continue;
                    }
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
        JourneyPresentationKeyResolver.clear();
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
                JourneyPresentationKeyResolver.unregister(variant);
            }
        }
        ownedVariants.clear();
    }

    private static boolean nativeExactPresent(ItemStack original, ResearchKey key, List<ItemStack> previousOwned) {
        try {
            for (ItemStack candidate : ItemList.itemMap.get(original.getItem())) {
                if (candidate == null || candidate.getItem() == null || containsIdentity(previousOwned, candidate)) continue;
                try {
                    if (key.equals(JourneyPresentationKeyResolver.keyOf(candidate))) return true;
                } catch (IllegalArgumentException ignored) {
                } catch (RuntimeException ignored) {
                } catch (LinkageError ignored) {}
            }
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}
        return false;
    }

    private static boolean containsIdentity(List<ItemStack> stacks, ItemStack candidate) {
        if (stacks == null) return false;
        for (ItemStack stack : stacks) if (stack == candidate) return true;
        return false;
    }

    private static Set<ResearchKey> keysOf(List<ItemStack> stacks) {
        Set<ResearchKey> out = new LinkedHashSet<ResearchKey>();
        if (stacks == null) return out;
        for (ItemStack stack : stacks) {
            ResearchKey key = authoritativeKey(stack);
            if (key != null) out.add(key);
        }
        return out;
    }

    private static ResearchKey authoritativeKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            return ItemStackKeyFactory.from(stack);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
