package dev.gtnhjourney.mixin;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.GuiRecipe;

/**
 * NEI's recipe.back binding checks only whether Backspace is down, so Ctrl+Backspace can navigate away from a recipe
 * screen after normal text/input handling has finished. Cancel only that modified chord immediately before NEI evaluates
 * recipe.back. Plain Backspace keeps its native recipe-history behavior, and a focused recipe search field still handles
 * Ctrl+Backspace earlier in GuiRecipe.keyTyped().
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
    private void gtnhjourney$doNotTreatCtrlBackspaceAsRecipeBack(char character, int keyCode, CallbackInfo ci) {
        if (keyCode == Keyboard.KEY_BACK && NEIClientUtils.controlKey()) ci.cancel();
    }
}
