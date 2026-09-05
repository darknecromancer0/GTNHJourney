package dev.gtnhjourney.nei;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;

/** Native NEI order/family metadata. A family is one registry item across its native subtype states. */
final class JourneyNativeFamilyIndex {

    private final Map<ResearchKey, Integer> exactIndex = new HashMap<ResearchKey, Integer>();
    private final Map<String, Integer> metaIndex = new HashMap<String, Integer>();
    private final Map<String, Integer> familyIndex = new HashMap<String, Integer>();

    JourneyNativeFamilyIndex(List<ItemStack> nativeItems) {
        if (nativeItems == null) return;
        for (int i = 0; i < nativeItems.size(); i++) {
            ItemStack stack = nativeItems.get(i);
            ResearchKey key = safeKey(stack);
            if (key == null) continue;
            if (!exactIndex.containsKey(key)) exactIndex.put(key, Integer.valueOf(i));
            String meta = metaKey(key);
            if (!metaIndex.containsKey(meta)) metaIndex.put(meta, Integer.valueOf(i));
            if (!familyIndex.containsKey(key.getItemId())) familyIndex.put(key.getItemId(), Integer.valueOf(i));
        }
    }

    int nativeIndex(ItemStack display, ResearchKey key) {
        ResearchKey effective = key == null ? safeKey(display) : key;
        if (effective == null) return Integer.MAX_VALUE;
        Integer exact = exactIndex.get(effective);
        if (exact != null) return exact.intValue();
        Integer meta = metaIndex.get(metaKey(effective));
        if (meta != null) return meta.intValue();
        Integer family = familyIndex.get(effective.getItemId());
        return family == null ? Integer.MAX_VALUE : family.intValue();
    }

    String familyKey(ItemStack display, ResearchKey key) {
        ResearchKey effective = key == null ? safeKey(display) : key;
        return effective == null ? "<invalid>" : effective.getItemId();
    }

    private static String metaKey(ResearchKey key) {
        return key.getItemId() + '\u0000' + key.getMeta();
    }

    private static ResearchKey safeKey(ItemStack stack) {
        try { return stack == null || stack.getItem() == null ? null : ItemStackKeyFactory.from(stack); }
        catch (RuntimeException ignored) { return null; }
        catch (LinkageError ignored) { return null; }
    }
}
