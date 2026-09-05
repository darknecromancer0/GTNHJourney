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
 * NEI normally dispatches global Minecraft keybinds after forwarding a key to its focused search field. For
 * Ctrl+Backspace that can both delete a word and trigger an unrelated GUI/keybind action. Consume this one chord at the
 * NEI manager boundary after giving it to the focused search field, so the same event cannot leak into
 * Minecraft.dispatchKeypresses().
 */
@Mixin(value = GuiContainerManager.class, remap = false)
public abstract class GuiContainerSearchCtrlBackspaceMixin {

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnhjourney$consumeSearchCtrlBackspace(CallbackInfo ci) {
        if (Keyboard.getEventKey() != Keyboard.KEY_BACK || !NEIClientUtils.controlKey()) return;
        if (LayoutManager.searchField == null || !LayoutManager.searchField.isVisible()
            || !LayoutManager.searchField.focused()) return;

        int key = Keyboard.getEventKey();
        char character = Keyboard.getEventCharacter();
        if (Keyboard.getEventKeyState() || (key == 0 && Character.isDefined(character))) {
            GuiContainerManager manager = (GuiContainerManager) (Object) this;
            manager.keyTyped(character, key);
        }
        ci.cancel();
    }
}
