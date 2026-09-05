package dev.gtnhjourney.mixin;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.ItemPanel;
import dev.gtnhjourney.nei.JourneyPanelController;

/**
 * Lets NEI finish its expensive native search on the NEI worker, then gives Journey the same worker window to build
 * its filtered/sorted presentation. The ready Journey list is published on the next client tick, never on a keypress.
 */
@Mixin(value = ItemPanel.class, remap = false)
public abstract class ItemPanelJourneyFilterMixin {

    @Inject(method = "updateItemList", at = @At("HEAD"), cancellable = true, remap = false)
    private static void gtnhjourney$captureCompletedFilter(ArrayList<ItemStack> newItems, CallbackInfo ci) {
        if (JourneyPanelController.captureCompletedNativeFilter(newItems)) ci.cancel();
    }
}
