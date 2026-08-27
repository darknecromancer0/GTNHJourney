package dev.gtnhjourney.client;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiScreenEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.gtnhjourney.command.JourneyCommandSuggestions;

/** Draws lightweight modern-style command suggestions above the 1.7.10 chat input. */
public final class JourneyCommandHintOverlay {

    private static final int MAX_VISIBLE = 10;
    private static Field inputField;
    private static boolean inputFieldResolved;

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) return;
        GuiTextField chatInput = chatInput((GuiChat) event.gui);
        if (chatInput == null) return;

        List<String> suggestions = JourneyCommandSuggestions.forChatText(chatInput.getText());
        if (suggestions.isEmpty()) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null) return;
        FontRenderer font = minecraft.fontRenderer;
        int shown = Math.min(MAX_VISIBLE, suggestions.size());
        int extra = suggestions.size() - shown;
        int lineHeight = font.FONT_HEIGHT + 2;
        int lines = shown + (extra > 0 ? 1 : 0);
        int maxWidth = 0;
        for (int i = 0; i < shown; i++) maxWidth = Math.max(maxWidth, font.getStringWidth(suggestions.get(i)));
        if (extra > 0) maxWidth = Math.max(maxWidth, font.getStringWidth("+" + extra + " more"));

        int left = 3;
        int bottom = event.gui.height - 17;
        int top = Math.max(3, bottom - lines * lineHeight - 4);
        Gui.drawRect(left - 2, top - 2, left + maxWidth + 5, bottom, 0xA0000000);
        int y = top;
        for (int i = 0; i < shown; i++) {
            font.drawStringWithShadow(suggestions.get(i), left, y, 0xFFE0E0E0);
            y += lineHeight;
        }
        if (extra > 0) font.drawStringWithShadow("+" + extra + " more", left, y, 0xFF909090);
    }

    private static GuiTextField chatInput(GuiChat chat) {
        if (!inputFieldResolved) resolveInputField();
        if (inputField == null) return null;
        try {
            return (GuiTextField) inputField.get(chat);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static synchronized void resolveInputField() {
        if (inputFieldResolved) return;
        inputFieldResolved = true;
        for (Field field : GuiChat.class.getDeclaredFields()) {
            if (!GuiTextField.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                inputField = field;
                return;
            } catch (RuntimeException ignored) {
                // Keep looking; an unusual security manager must not break chat rendering.
            }
        }
    }
}
