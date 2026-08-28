package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import codechicken.nei.ItemList;
import codechicken.nei.LayoutManager;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.api.ItemFilter.ItemFilterProvider;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;

/** Applies NEI's active item filters to Journey's already ordered item list. */
final class JourneyNeiFilterPipeline {

    private JourneyNeiFilterPipeline() {}

    static List<ItemFilter> snapshotActiveFilters() {
        List<ItemFilter> filters = new ArrayList<ItemFilter>();
        synchronized (ItemList.itemFilterers) {
            for (ItemFilterProvider provider : ItemList.itemFilterers) {
                if (provider == null || provider instanceof JourneyItemFilterProvider) continue;
                if (provider == LayoutManager.searchField && !LayoutManager.searchField.isVisible()) continue;
                try {
                    ItemFilter filter = provider.getFilter();
                    if (filter != null) filters.add(filter);
                } catch (Throwable ignored) {
                    JourneyRuntimeCounters.presentationFailure();
                }
            }
        }
        return filters;
    }

    static boolean matchesAll(ItemStack stack, List<ItemFilter> filters) {
        if (stack == null || filters == null) return false;
        for (ItemFilter filter : filters) {
            if (filter == null) continue;
            try {
                if (!filter.matches(stack)) return false;
            } catch (Throwable ignored) {
                JourneyRuntimeCounters.presentationFailure();
            }
        }
        return true;
    }
}
