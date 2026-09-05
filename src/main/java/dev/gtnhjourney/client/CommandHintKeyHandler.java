package dev.gtnhjourney.client;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

/** Handles popup command navigation without stealing vanilla chat-history arrows. */
public final class CommandHintKeyHandler {

    private static final int KEY_TAB = 15;
    private static final int KEY_UP = 200;
    private static final int KEY_DOWN = 208;

    private CommandHintKeyHandler() {}

    public static boolean handle(GuiChat chat, char typedChar, int keyCode) {
        if (chat == null || !ClientCommandSuggestionState.hasSuggestions()) return false;
        GuiTextField input = ChatInputResolver.resolve(chat);
        if (input == null) return false;
        boolean shiftDown = GuiScreen.isShiftKeyDown();
        if (shouldMoveSelection(keyCode, shiftDown)) {
            return ClientCommandSuggestionState.moveSelection(keyCode == KEY_DOWN ? 1 : -1);
        }
        if (keyCode == KEY_TAB) return ClientCommandSuggestionState.acceptSelected(input);
        return false;
    }

    public static boolean shouldMoveSelection(int keyCode, boolean shiftDown) {
        return shiftDown && (keyCode == KEY_UP || keyCode == KEY_DOWN);
    }
}
