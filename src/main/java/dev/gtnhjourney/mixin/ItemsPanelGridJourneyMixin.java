package dev.gtnhjourney.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import codechicken.nei.CollapsibleItems;
import codechicken.nei.ItemsPanelGrid;
import dev.gtnhjourney.nei.JourneyViewState;

/** Keeps exact researched registry items separate while Journey owns the NEI item panel. */
@Mixin(value = ItemsPanelGrid.class, remap = false)
public abstract class ItemsPanelGridJourneyMixin {

    @Redirect(
        method = "refresh",
        at = @At(value = "INVOKE", target = "Lcodechicken/nei/CollapsibleItems;isEmpty()Z"),
        require = 0)
    private boolean gtnhjourney$disableCollapsibleGroupsForJourney() {
        boolean originalEmpty = CollapsibleItems.isEmpty();
        boolean journeyActive = JourneyViewState.isEnabled();
        return journeyActive || originalEmpty;
    }
}
