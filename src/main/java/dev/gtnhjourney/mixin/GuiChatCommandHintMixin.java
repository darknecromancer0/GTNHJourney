package dev.gtnhjourney.mixin;

import net.minecraft.client.gui.GuiChat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.gtnhjourney.client.CommandHintKeyHandler;

/** Lets the global command popup own Up/Down/Tab only while it actually has suggestions. */
@Mixin(GuiChat.class)
public abstract class GuiChatCommandHintMixin {

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void gtnhjourney$commandHints(char typedChar, int keyCode, CallbackInfo ci) {
        if (CommandHintKeyHandler.handle((GuiChat) (Object) this, typedChar, keyCode)) ci.cancel();
    }
}
