package dev.gtnhjourney.nei;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Maps renderer-safe NEI presentation stacks back to their authoritative researched state. */
final class JourneyPresentationKeyResolver {

    private static final String MARKER = "GTNHJourneyPresentation";
    private static final Map<ItemStack, ResearchKey> presentationKeys = new IdentityHashMap<ItemStack, ResearchKey>();
    private static final Map<Integer, ResearchKey> markerKeys = new HashMap<Integer, ResearchKey>();
    private static int nextMarker = 1;

    private JourneyPresentationKeyResolver() {}

    static synchronized void register(ItemStack presentation, ResearchKey key) {
        if (presentation == null || presentation.getItem() == null || key == null) return;
        int marker = nextMarker++;
        if (marker <= 0) {
            markerKeys.clear();
            nextMarker = 2;
            marker = 1;
        }
        NBTTagCompound tag = presentation.hasTagCompound()
            ? (NBTTagCompound) presentation.getTagCompound().copy()
            : new NBTTagCompound();
        tag.setInteger(MARKER, marker);
        presentation.setTagCompound(tag);
        presentationKeys.put(presentation, key);
        markerKeys.put(Integer.valueOf(marker), key);
    }

    static synchronized void unregister(ItemStack presentation) {
        if (presentation == null) return;
        presentationKeys.remove(presentation);
        if (presentation.hasTagCompound() && presentation.getTagCompound().hasKey(MARKER, 3)) {
            markerKeys.remove(Integer.valueOf(presentation.getTagCompound().getInteger(MARKER)));
        }
    }

    static synchronized void clear() {
        presentationKeys.clear();
        markerKeys.clear();
        nextMarker = 1;
    }

    static ResearchKey keyOf(ItemStack stack) {
        if (stack == null || stack.getItem() == null) throw new IllegalArgumentException("stack and item must not be null");
        synchronized (JourneyPresentationKeyResolver.class) {
            ResearchKey mapped = presentationKeys.get(stack);
            if (mapped != null) return mapped;
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey(MARKER, 3)) {
                mapped = markerKeys.get(Integer.valueOf(stack.getTagCompound().getInteger(MARKER)));
                if (mapped != null) return mapped;
            }
        }
        return ItemStackKeyFactory.from(stack);
    }
}
