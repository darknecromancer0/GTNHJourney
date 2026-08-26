package dev.gtnhjourney.acquisition;

import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import net.minecraft.item.ItemStack;

/** Filters internal migration helpers out of normal player research observations. */
public final class ResearchObservationPolicy {

    private ResearchObservationPolicy() {}

    public static boolean shouldObserve(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.stackSize > 0
            && !(stack.getItem() instanceof ItemDebugResearcherTool);
    }
}
