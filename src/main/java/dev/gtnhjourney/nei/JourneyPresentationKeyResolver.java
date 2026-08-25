package dev.gtnhjourney.nei;

import java.util.IdentityHashMap;
import java.util.Map;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.ItemStack;

/** Maps renderer-safe NEI presentation stacks back to their authoritative researched state. */
final class JourneyPresentationKeyResolver {

    private static final Map<ItemStack, ResearchKey> presentationKeys = new IdentityHashMap<ItemStack, ResearchKey>();

    private JourneyPresentationKeyResolver() {}

    static synchronized void register(ItemStack presentation, ResearchKey key) {
        if (presentation == null || presentation.getItem() == null || key == null) return;
        presentationKeys.put(presentation, key);
    }

    static synchronized void unregister(ItemStack presentation) {
        if (presentation != null) presentationKeys.remove(presentation);
    }

    static synchronized void clear() {
        presentationKeys.clear();
    }

    static synchronized boolean isPresentation(ItemStack stack) {
        return stack != null && stack.getItem() != null && presentationKeys.containsKey(stack);
    }

    static ResearchKey keyOf(ItemStack stack) {
        if (stack == null || stack.getItem() == null) throw new IllegalArgumentException("stack and item must not be null");
        synchronized (JourneyPresentationKeyResolver.class) {
            ResearchKey mapped = presentationKeys.get(stack);
            if (mapped != null) return mapped;
        }
        return ItemStackKeyFactory.from(stack);
    }
}
