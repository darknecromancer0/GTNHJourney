package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.ItemFilter;

/** Keeps stock/Creative NEI usable by omitting only a proven crash-inducing GT volumetric-flask display state. */
final class JourneyGlobalSafetyFilter implements ItemFilter {

    @Override
    public boolean matches(ItemStack item) {
        if (!JourneyPresentationSafety.isVolumetricFlask(item)) return true;
        boolean hasFluid = item != null && item.hasTagCompound() && item.getTagCompound().hasKey("Fluid", 10);
        boolean unsafe = hasFluid && JourneyPresentationSafety.hasUnsafeFluidIcon(item);
        String className = item == null || item.getItem() == null ? "" : item.getItem().getClass().getName();
        return !JourneyGlobalSafetyPolicy.shouldHide(className, hasFluid, unsafe);
    }
}
