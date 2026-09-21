package dev.gtnhjourney.mixin;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.guihook.GuiContainerManager;

/**
 * Lets NEI's focused search field process Ctrl+Backspace through its normal TextField path, then consumes the event
 * before Minecraft's global key dispatcher can also treat Backspace as a GUI/keybind action.
 *
 * <p>The older Journey guard intercepted at HEAD and manually re-entered NEI's key handler. That avoided the historical
 * recipe-GUI crash, but bypassed part of NEI's normal keyboard-input lifecycle and could intermittently leave the
 * search field looking focused while subsequent typing was no longer accepted until focus was cycled with the mouse.</p>
 */
@Mixin(value = GuiContainerManager.class, remap = false)
public abstract class GuiContainerSearchCtrlBackspaceMixin {

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
        if (Keyboard.getEventKey() != Keyboard.KEY_BACK || !NEIClientUtils.controlKey()) return;
        if (LayoutManager.searchField == null || !LayoutManager.searchField.isVisible()
            || !LayoutManager.searchField.focused()) return;

        // NEI has already performed textboxKeyTyped() and onTextChange() at this point. Only suppress the later
        // Minecraft.dispatchKeypresses() equivalent so focus/repeat ownership stays entirely native.
        ci.cancel();
    }
}
