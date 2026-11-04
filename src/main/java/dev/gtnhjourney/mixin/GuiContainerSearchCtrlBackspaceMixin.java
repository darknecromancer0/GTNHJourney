package dev.gtnhjourney.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import codechicken.nei.guihook.GuiContainerManager;
import dev.gtnhjourney.nei.JourneySearchFocusKeeper;

/**
 * Capture search ownership before NEI processes Ctrl+Backspace. The search may lose focus while the native input
 * updates/rebuilds its panel, so checking focused() only after keyTyped() is too late and leaves the global Minecraft
 * key dispatcher free to steal input. Keep native textbox editing, suppress global dispatch and retain the original
 * search focus until the next intentional input or mouse click.
 */
@Mixin(value = GuiContainerManager.class, remap = false)
public abstract class GuiContainerSearchCtrlBackspaceMixin {

    @Unique
    private boolean gtnhjourney$consumeCtrlBackspace;

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), remap = false)
    private void gtnhjourney$captureSearchFocusBeforeNativeInput(CallbackInfo ci) {
        gtnhjourney$consumeCtrlBackspace = JourneySearchFocusKeeper.beforeKeyEvent();
    }

    @Inject(
        method = "handleKeyboardInput",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/guihook/GuiContainerManager;keyTyped(CI)V",
            shift = At.Shift.AFTER,
            remap = false),
        cancellable = true,
        remap = false)
    private void gtnhjourney$consumeSearchCtrlBackspaceAfterNativeInput(CallbackInfo ci) {
        if (!gtnhjourney$consumeCtrlBackspace) return;
        gtnhjourney$consumeCtrlBackspace = false;
        JourneySearchFocusKeeper.afterNativeCtrlBackspace();
        ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), remap = false)
    private void gtnhjourney$releaseSearchFocusOnClick(
        int mousex, int mousey, int button, CallbackInfoReturnable<Boolean> cir) {
        JourneySearchFocusKeeper.onMouseClick();
    }
}
