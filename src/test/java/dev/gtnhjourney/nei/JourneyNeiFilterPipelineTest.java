package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.nei.api.ItemFilter;
import codechicken.nei.api.ItemFilter.ItemFilterProvider;

public class JourneyNeiFilterPipelineTest {

    @Test
    public void keepsNativeProvidersButSkipsJourneyAndHiddenSearchProviders() {
        ItemFilter nativeFilter = item -> true;
        ItemFilter searchFilter = item -> true;
        ItemFilterProvider nativeProvider = () -> nativeFilter;
        ItemFilterProvider searchProvider = () -> searchFilter;
        ItemFilterProvider journeyProvider = new JourneyItemFilterProvider();

        List<ItemFilter> filters = JourneyNeiFilterPipeline.snapshotFilters(
            Arrays.asList(nativeProvider, searchProvider, journeyProvider), searchProvider, false);

        assertEquals(1, filters.size());
        assertSame(nativeFilter, filters.get(0));
    }

    @Test
    public void includesVisibleSearchProviderSoNeiPrefixesStayNative() {
        ItemFilter nativeFilter = item -> true;
        ItemFilter searchFilter = item -> true;
        ItemFilterProvider nativeProvider = () -> nativeFilter;
        ItemFilterProvider searchProvider = () -> searchFilter;

        List<ItemFilter> filters = JourneyNeiFilterPipeline.snapshotFilters(
            Arrays.asList(nativeProvider, searchProvider), searchProvider, true);

        assertEquals(2, filters.size());
        assertSame(nativeFilter, filters.get(0));
        assertSame(searchFilter, filters.get(1));
    }
}
