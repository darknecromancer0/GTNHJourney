package dev.gtnhjourney.client;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;

/** Handles popup-only command navigation without stealing vanilla chat keys when no suggestions are visible. */
public final class CommandHintKeyHandler {

    private static final int KEY_TAB = 15;
    private static final int KEY_UP = 200;
    private static final int KEY_DOWN = 208;

    private CommandHintKeyHandler() {}

    public static boolean handle(GuiChat chat, char typedChar, int keyCode) {
        if (chat == null || !ClientCommandSuggestionState.hasSuggestions()) return false;
        GuiTextField input = ChatInputResolver.resolve(chat);
        if (input == null) return false;
        if (keyCode == KEY_DOWN) return ClientCommandSuggestionState.moveSelection(1);
        if (keyCode == KEY_UP) return ClientCommandSuggestionState.moveSelection(-1);
        if (keyCode == KEY_TAB) return ClientCommandSuggestionState.acceptSelected(input);
        return false;
    }
}
