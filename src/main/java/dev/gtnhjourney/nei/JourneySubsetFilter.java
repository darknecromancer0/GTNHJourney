package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.ItemFilter;
import dev.gtnhjourney.client.ClientResearchMirror;

/** NEI subset filter backed by the server-synchronized research mirror. */
public final class JourneySubsetFilter implements ItemFilter {

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItem() == null) return false;
        try {
            return ClientResearchMirror.contains(JourneyPresentationKeyResolver.keyOf(item));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
