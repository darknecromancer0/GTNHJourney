package dev.gtnhjourney.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.CollapsibleItems;
import codechicken.nei.ItemsPanelGrid;
import dev.gtnhjourney.nei.JourneyViewState;
import dev.gtnhjourney.nei.JourneyFavouriteStarOverlay;

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

    /** After the NEI item grid and its framebuffer cache have rendered, paint tiny, live favourite markers. */
    @Inject(method = "draw", at = @At("RETURN"), remap = false)
    private void gtnhjourney$drawFavouriteStarMarkers(int mousex, int mousey, CallbackInfo ci) {
        JourneyFavouriteStarOverlay.render((ItemsPanelGrid) (Object) this);
    }
}

