package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;

/** Exact policy for the one proven GT volumetric-flask renderer failure. */
final class JourneyGlobalSafetyPolicy {

    private static final String VOLUMETRIC_FLASK = "gregtech.common.items.ItemVolumetricFlask";

    private JourneyGlobalSafetyPolicy() {}

    static boolean shouldHide(String itemClassName, boolean hasFluidPayload, boolean unsafeFluidIcon) {
        return VOLUMETRIC_FLASK.equals(itemClassName) && hasFluidPayload && unsafeFluidIcon;
    }

    static boolean shouldHide(ItemStack item) {
        if (!JourneyPresentationSafety.isVolumetricFlask(item)) return false;
        boolean hasFluid = item != null && item.hasTagCompound() && item.getTagCompound().hasKey("Fluid", 10);
        boolean unsafe = hasFluid && JourneyPresentationSafety.hasUnsafeFluidIcon(item);
        String className = item == null || item.getItem() == null ? "" : item.getItem().getClass().getName();
        return shouldHide(className, hasFluid, unsafe);
    }
}
