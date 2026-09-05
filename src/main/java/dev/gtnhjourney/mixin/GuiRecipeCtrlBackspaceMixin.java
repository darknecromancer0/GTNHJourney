package dev.gtnhjourney.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.KeyManager;
import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.GuiRecipe;
import dev.gtnhjourney.nei.JourneyRecipeBackGuard;

/**
 * NEI's recipe.back check reads the physical key state, not the current keyTyped event. While Backspace is held, a
 * repeat or unrelated keyboard event can therefore reach recipe.back even after the original Ctrl+Backspace event was
 * consumed. Suppress recipe navigation whenever recipe.back is physically down and Ctrl is still held or the main NEI
 * search field still owns keyboard focus. Plain Backspace outside search keeps native recipe-history behavior.
 */
@Mixin(value = GuiRecipe.class, remap = false)
public abstract class GuiRecipeCtrlBackspaceMixin {

    @Inject(
        method = "keyTyped",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/KeyManager;isKeyDown(Ljava/lang/String;)Z",
            ordinal = 2,
            remap = false),
        cancellable = true,
        remap = true)
    private void gtnhjourney$doNotTreatSearchBackspaceAsRecipeBack(char character, int keyCode, CallbackInfo ci) {
        boolean mainSearchFocused = LayoutManager.searchField != null && LayoutManager.searchField.isVisible()
            && LayoutManager.searchField.focused();
        if (JourneyRecipeBackGuard.shouldSuppress(
            KeyManager.isKeyDown("recipe.back"),
            NEIClientUtils.controlKey(),
            mainSearchFocused)) {
            ci.cancel();
        }
    }
}
