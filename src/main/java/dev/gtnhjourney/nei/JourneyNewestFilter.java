package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.ItemFilter;

/**
 * Legacy NEI subset name retained for config compatibility. N is no longer a subset: its membership is identical to J
 * and only its ordering differs, so this filter delegates to the researched-set membership policy.
 */
public final class JourneyNewestFilter implements ItemFilter {

    private final JourneySubsetFilter researched = new JourneySubsetFilter();

    @Override
    public boolean matches(ItemStack item) {
        return researched.matches(item);
    }
}
