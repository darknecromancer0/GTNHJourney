package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import codechicken.nei.ItemSorter;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.ItemStack;

/** Temporarily places Journey's research chronology ahead of the user's normal NEI sort while J/N is active. */
final class JourneyNEIOrderBridge {

    private static final String SORT_NAME = "gtnhjourney.internal.research_order";
    private static volatile Map<ResearchKey, Integer> ranks = Collections.emptyMap();
    private static final Map<ItemStack, Integer> cachedStackRanks = new IdentityHashMap<ItemStack, Integer>();

    private static final ItemSorter.SortEntry JOURNEY_SORT = new ItemSorter.SortEntry(
        SORT_NAME,
        new Comparator<ItemStack>() {

            @Override
            public int compare(ItemStack left, ItemStack right) {
                return Integer.compare(rank(left), rank(right));
            }
        });

    private JourneyNEIOrderBridge() {}

    static synchronized boolean update(JourneyViewState.Mode mode) {
        JourneyViewState.Mode effective = mode == null ? JourneyViewState.Mode.ALL : mode;
        Map<ResearchKey, Integer> nextRanks = buildRanks(effective);
        boolean ranksChanged = !nextRanks.equals(ranks);
        if (ranksChanged) {
            ranks = Collections.unmodifiableMap(nextRanks);
            cachedStackRanks.clear();
        }
        return installSorter(effective != JourneyViewState.Mode.ALL) || ranksChanged;
    }

    static synchronized boolean reset() {
        boolean ranksChanged = !ranks.isEmpty();
        ranks = Collections.emptyMap();
        cachedStackRanks.clear();
        return installSorter(false) || ranksChanged;
    }

    private static Map<ResearchKey, Integer> buildRanks(JourneyViewState.Mode mode) {
        if (mode == JourneyViewState.Mode.ALL) return Collections.emptyMap();
        List<ResearchKey> oldestFirst = new ArrayList<ResearchKey>();
        for (ItemStack stack : ClientStackMirror.snapshot()) {
            try {
                oldestFirst.add(ItemStackKeyFactory.from(stack));
            } catch (IllegalArgumentException ignored) {}
        }
        List<ResearchKey> ordered = JourneyPanelOrder.keysForMode(oldestFirst, mode, JourneyConfig.newestLimit());
        Map<ResearchKey, Integer> out = new LinkedHashMap<ResearchKey, Integer>();
        for (int i = 0; i < ordered.size(); i++) out.put(ordered.get(i), Integer.valueOf(i));
        return out;
    }

    private static boolean installSorter(boolean active) {
        ArrayList<ItemSorter.SortEntry> current = ItemSorter.list;
        int firstInternal = -1;
        int internalCount = 0;
        for (int i = 0; i < current.size(); i++) {
            ItemSorter.SortEntry entry = current.get(i);
            if (entry != null && SORT_NAME.equals(entry.name)) {
                if (firstInternal < 0) firstInternal = i;
                internalCount++;
            }
        }
        boolean alreadyCorrect = active
            ? internalCount == 1 && firstInternal == 0 && current.get(0) == JOURNEY_SORT
            : internalCount == 0;
        if (alreadyCorrect) return false;

        ArrayList<ItemSorter.SortEntry> next = new ArrayList<ItemSorter.SortEntry>();
        for (ItemSorter.SortEntry entry : current) {
            if (entry == null || !SORT_NAME.equals(entry.name)) next.add(entry);
        }
        if (active) next.add(0, JOURNEY_SORT);
        ItemSorter.list = next;
        return true;
    }

    private static int rank(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return Integer.MAX_VALUE;
        synchronized (JourneyNEIOrderBridge.class) {
            Integer cached = cachedStackRanks.get(stack);
            if (cached != null) return cached.intValue();
            int value = Integer.MAX_VALUE;
            try {
                Integer resolved = ranks.get(JourneyPresentationKeyResolver.keyOf(stack));
                if (resolved != null) value = resolved.intValue();
            } catch (IllegalArgumentException ignored) {
            } catch (RuntimeException ignored) {
            } catch (LinkageError ignored) {}
            cachedStackRanks.put(stack, Integer.valueOf(value));
            return value;
        }
    }
}
