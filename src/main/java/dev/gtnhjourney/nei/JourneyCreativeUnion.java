package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;

/** Keeps native NEI order and inserts missing exact Journey variants beside their native item/meta family. */
final class JourneyCreativeUnion {

    private JourneyCreativeUnion() {}

    static ArrayList<ItemStack> merge(List<ItemStack> nativeItems, List<ItemStack> journeyItems) {
        ArrayList<ItemStack> nativeCopy = validCopies(nativeItems);
        Set<ResearchKey> nativeExact = new HashSet<ResearchKey>();
        Map<String, Integer> lastFamilyIndex = new HashMap<String, Integer>();
        for (int i = 0; i < nativeCopy.size(); i++) {
            ItemStack stack = nativeCopy.get(i);
            ResearchKey key = safeKey(stack);
            if (key != null) nativeExact.add(key);
            lastFamilyIndex.put(family(stack), Integer.valueOf(i));
        }

        Map<String, List<ItemStack>> extrasByFamily = new LinkedHashMap<String, List<ItemStack>>();
        List<ItemStack> orphanExtras = new ArrayList<ItemStack>();
        Set<ResearchKey> extrasSeen = new HashSet<ResearchKey>();
        if (journeyItems != null) {
            for (ItemStack raw : journeyItems) {
                if (!valid(raw)) continue;
                ResearchKey key = safeKey(raw);
                if (key == null || nativeExact.contains(key) || !extrasSeen.add(key)) continue;
                ItemStack display = JourneyPresentationSafety.forNei(raw);
                if (!valid(display)) continue;
                String family = family(display);
                if (!lastFamilyIndex.containsKey(family)) {
                    orphanExtras.add(display);
                    continue;
                }
                List<ItemStack> list = extrasByFamily.get(family);
                if (list == null) {
                    list = new ArrayList<ItemStack>();
                    extrasByFamily.put(family, list);
                }
                list.add(display);
            }
        }

        ArrayList<ItemStack> out = new ArrayList<ItemStack>(nativeCopy.size() + extrasSeen.size());
        for (int i = 0; i < nativeCopy.size(); i++) {
            ItemStack stack = nativeCopy.get(i);
            out.add(stack);
            String family = family(stack);
            Integer last = lastFamilyIndex.get(family);
            if (last == null || last.intValue() != i) continue;
            List<ItemStack> extras = extrasByFamily.get(family);
            if (extras != null) out.addAll(extras);
        }
        out.addAll(orphanExtras);
        return out;
    }

    private static ArrayList<ItemStack> validCopies(List<ItemStack> source) {
        ArrayList<ItemStack> out = new ArrayList<ItemStack>();
        if (source == null) return out;
        for (ItemStack stack : source) if (valid(stack)) out.add(stack.copy());
        return out;
    }

    private static String family(ItemStack stack) {
        ResearchKey key = safeKey(stack);
        if (key != null) return key.getItemId() + "\u0000" + key.getMeta();
        return stack == null || stack.getItem() == null ? "<invalid>" : stack.getItem().getUnlocalizedName() + "\u0000" + stack.getItemDamage();
    }

    private static ResearchKey safeKey(ItemStack stack) {
        try { return valid(stack) ? ItemStackKeyFactory.from(stack) : null; }
        catch (RuntimeException ignored) { return null; }
        catch (LinkageError ignored) { return null; }
    }

    private static boolean valid(ItemStack stack) { return stack != null && stack.getItem() != null && stack.stackSize > 0; }
}
