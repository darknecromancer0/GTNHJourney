package dev.gtnhjourney.acquisition;

import dev.gtnhjourney.debug.ItemBotaniaManaDebugTool;
import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import dev.gtnhjourney.minecraft.GtToolStatePolicy;
import net.minecraft.item.ItemStack;

/** Filters internal helpers and positively invalid runtime states out of new player research observations. */
public final class ResearchObservationPolicy {

    private ResearchObservationPolicy() {}

    public static boolean shouldObserve(ItemStack stack) {
        return shouldPersist(stack) && !GtToolStatePolicy.isKnownInvalidToolState(stack);
    }

    /**
     * Persistence migration is deliberately less strict than new observation. Legacy research history stays loadable
     * even when the current optional-mod runtime no longer exposes that old state for display or issuance.
     */
    public static boolean shouldPersist(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.stackSize > 0
            && !(stack.getItem() instanceof ItemDebugResearcherTool)
            && !(stack.getItem() instanceof ItemBotaniaManaDebugTool);
    }
}
