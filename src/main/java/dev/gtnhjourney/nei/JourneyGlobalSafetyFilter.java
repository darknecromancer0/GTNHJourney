package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.ItemFilter;

/** Keeps NEI usable by omitting only a proven crash-inducing GT volumetric-flask display state. */
final class JourneyGlobalSafetyFilter implements ItemFilter {

    @Override
    public boolean matches(ItemStack item) {
        return !JourneyGlobalSafetyPolicy.shouldHide(item);
    }
}
