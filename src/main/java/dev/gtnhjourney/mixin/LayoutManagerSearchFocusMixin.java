package dev.gtnhjourney.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.LayoutManager;
import dev.gtnhjourney.nei.JourneySearchFocusKeeper;

/** Reasserts previously owned search focus after a NEI layout during an active Ctrl+Backspace focus lease. */
@Mixin(value = LayoutManager.class, remap = false)
public abstract class LayoutManagerSearchFocusMixin {

    @Inject(method = "layout", at = @At("RETURN"), remap = false)
    private static void gtnhjourney$preserveSearchFocusAfterNeiLayout(GuiContainer gui, CallbackInfo ci) {
        JourneySearchFocusKeeper.afterLayout();
    }
}
