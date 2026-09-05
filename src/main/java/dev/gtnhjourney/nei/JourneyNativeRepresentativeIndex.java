package dev.gtnhjourney.nei;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;

/** Resolves Journey presentation stacks back to a native NEI universe member when possible. */
final class JourneyNativeRepresentativeIndex {

    private final Map<ResearchKey, ItemStack> exact = new HashMap<ResearchKey, ItemStack>();
    private final Map<String, ItemStack> families = new HashMap<String, ItemStack>();

    JourneyNativeRepresentativeIndex(List<ItemStack> nativeItems) {
        if (nativeItems == null) return;
        for (ItemStack stack : nativeItems) {
            if (stack == null || stack.getItem() == null) continue;
            ResearchKey key = safeKey(stack);
            if (key != null && !exact.containsKey(key)) exact.put(key, stack);
            String family = family(key, stack);
            if (!families.containsKey(family)) families.put(family, stack);
        }
    }

    ItemStack representative(ItemStack display) {
        ResearchKey key = safeKey(display);
        if (key != null) {
            ItemStack exactMatch = exact.get(key);
            if (exactMatch != null) return exactMatch;
        }
        ItemStack familyMatch = families.get(family(key, display));
        return familyMatch == null ? display : familyMatch;
    }

    private static ResearchKey safeKey(ItemStack stack) {
        try { return stack == null || stack.getItem() == null ? null : ItemStackKeyFactory.from(stack); }
        catch (RuntimeException ignored) { return null; }
        catch (LinkageError ignored) { return null; }
    }

    private static String family(ResearchKey key, ItemStack stack) {
        if (key != null) return key.getItemId() + "\u0000" + key.getMeta();
        return stack == null || stack.getItem() == null ? "<invalid>" : stack.getItem().getUnlocalizedName() + "\u0000" + stack.getItemDamage();
    }
}
