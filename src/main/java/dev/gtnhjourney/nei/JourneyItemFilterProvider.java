package dev.gtnhjourney.nei;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.ItemFilter;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;

/** Dynamic NEI mode filter. The returned filter captures immutable state as required by the NEI API. */
public final class JourneyItemFilterProvider implements ItemFilter.ItemFilterProvider {

    @Override
    public ItemFilter getFilter() {
        final JourneyViewState.Mode mode = JourneyViewState.mode();
        if (mode == JourneyViewState.Mode.ALL) return new ItemFilter() {

            @Override
            public boolean matches(ItemStack item) {
                return true;
            }
        };

        final Set<ResearchKey> allowed = new HashSet<ResearchKey>();
        if (mode == JourneyViewState.Mode.NEWEST) {
            for (ItemStack stack : ClientStackMirror.snapshotNewest(JourneyConfig.newestLimit())) {
                try {
                    allowed.add(ItemStackKeyFactory.from(stack));
                } catch (IllegalArgumentException ignored) {}
            }
        } else {
            allowed.addAll(ClientResearchMirror.snapshot());
        }

        return new ItemFilter() {

            @Override
            public boolean matches(ItemStack stack) {
                if (stack == null || stack.getItem() == null) return false;
                try {
                    return allowed.contains(JourneyPresentationKeyResolver.keyOf(stack));
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }
        };
    }
}
