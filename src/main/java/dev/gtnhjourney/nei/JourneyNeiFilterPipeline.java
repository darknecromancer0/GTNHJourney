package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import codechicken.nei.ItemList;
import codechicken.nei.LayoutManager;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.api.ItemFilter.ItemFilterProvider;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;

/** Applies native NEI query filters while keeping Journey-only presentation variants renderable. */
final class JourneyNeiFilterPipeline {

    private static final String SUBSET_WIDGET = "codechicken.nei.SubsetWidget";

    private JourneyNeiFilterPipeline() {}

    static List<FilterBinding> snapshotActiveFilters() {
        List<FilterBinding> filters = new ArrayList<FilterBinding>();
        List<String> providerNames = new ArrayList<String>();
        synchronized (ItemList.itemFilterers) {
            for (ItemFilterProvider provider : ItemList.itemFilterers) {
                if (provider == null || provider instanceof JourneyItemFilterProvider) continue;
                boolean searchField = provider == LayoutManager.searchField;
                String providerClassName = provider.getClass().getName();
                if (!JourneyNeiFilterProviderPolicy.shouldApply(
                    providerClassName,
                    searchField,
                    !searchField || LayoutManager.searchField.isVisible())) {
                    continue;
                }
                try {
                    ItemFilter filter = provider.getFilter();
                    if (filter != null) {
                        filters.add(new FilterBinding(providerClassName, filter, searchField));
                        providerNames.add(providerClassName);
                    }
                } catch (Throwable ignored) {
                    JourneyRuntimeCounters.presentationFailure();
                }
            }
        }
        JourneyFilterDiagnostics.record(providerNames, JourneyFilterDiagnostics.safeSearchText(LayoutManager.searchField));
        return filters;
    }

    static boolean matchesAll(ItemStack display, ItemStack nativeRepresentative, List<FilterBinding> filters) {
        if (display == null || filters == null) return false;
        String rawSearch = JourneyFilterDiagnostics.snapshot().searchText();
        for (FilterBinding binding : filters) {
            if (binding == null || binding.filter == null) continue;
            ItemStack candidate = binding.usesNativeRepresentative() && nativeRepresentative != null
                ? nativeRepresentative
                : display;
            try {
                boolean matched = binding.filter.matches(candidate);
                if (binding.searchField) {
                    matched = JourneyEmptySearchPolicy.resolveSearchMatch(rawSearch, display, matched);
                }
                if (!matched) return false;
            } catch (Throwable ignored) {
                JourneyRuntimeCounters.presentationFailure();
                return false;
            }
        }
        return true;
    }

    static final class FilterBinding {
        private final String providerClassName;
        private final ItemFilter filter;
        private final boolean searchField;

        FilterBinding(String providerClassName, ItemFilter filter) {
            this(providerClassName, filter, false);
        }

        FilterBinding(String providerClassName, ItemFilter filter, boolean searchField) {
            this.providerClassName = providerClassName == null ? "" : providerClassName;
            this.filter = filter;
            this.searchField = searchField;
        }

        boolean usesNativeRepresentative() {
            return SUBSET_WIDGET.equals(providerClassName) || providerClassName.endsWith(".SubsetWidget");
        }
    }
}
