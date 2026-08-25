package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.ItemFilter;

/** Journey's controller owns J/N selection; this provider must stay non-recursive for NEI's global filter chain. */
public final class JourneyItemFilterProvider implements ItemFilter.ItemFilterProvider {

    @Override
    public ItemFilter getFilter() {
        final JourneyViewState.Mode mode = JourneyViewState.mode();
        return new ItemFilter() {

            @Override
            public boolean matches(ItemStack item) {
                return JourneyItemFilterModePolicy.allowThrough(mode);
            }
        };
    }
}
