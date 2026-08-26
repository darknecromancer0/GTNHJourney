package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.ItemFilter;

/** Keeps NEI's own async search rebuild constrained to the active Journey view. */
public final class JourneyItemFilterProvider implements ItemFilter.ItemFilterProvider {

    private final JourneySubsetFilter researched = new JourneySubsetFilter();
    private final JourneyNewestFilter newest = new JourneyNewestFilter();

    @Override
    public ItemFilter getFilter() {
        JourneyViewState.Mode mode = JourneyViewState.mode();
        if (mode == JourneyViewState.Mode.RESEARCHED || mode == JourneyViewState.Mode.DELETE) return researched;
        if (mode == JourneyViewState.Mode.NEWEST) return newest;
        return new ItemFilter() {

            @Override
            public boolean matches(ItemStack item) {
                return true;
            }
        };
    }
}
