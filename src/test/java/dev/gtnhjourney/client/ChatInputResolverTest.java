package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;

import org.junit.jupiter.api.Test;

public class ChatInputResolverTest {

    @Test
    public void resolvesRealInputFromSuperclassWhenRuntimeGuiHasOtherFields() throws Exception {
        WrappedGuiChat chat = new WrappedGuiChat();
        GuiTextField expected = new GuiTextField(null, 0, 0, 10, 10);
        setGuiChatInput(chat, expected);

        assertSame(expected, ChatInputResolver.resolve(chat));
    }

    @Test
    public void ignoresNullGuiTextFieldDecoyBeforeSuperclassInput() throws Exception {
        GuiWithNullDecoy chat = new GuiWithNullDecoy();
        GuiTextField expected = new GuiTextField(null, 0, 0, 10, 10);
        setGuiChatInput(chat, expected);

        assertSame(expected, ChatInputResolver.resolve(chat));
    }

    @Test
    public void nullInputFailsOpenWithoutThrowing() {
        assertNull(ChatInputResolver.resolve(new WrappedGuiChat()));
    }

    private static void setGuiChatInput(GuiChat chat, GuiTextField input) throws Exception {
        for (Field field : GuiChat.class.getDeclaredFields()) {
            if (!GuiTextField.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            field.set(chat, input);
            return;
        }
        throw new AssertionError("GuiChat input field not found");
    }

    private static class WrappedGuiChat extends GuiChat {
        @SuppressWarnings("unused")
        private String unrelated = "before-input";
    }

    private static final class GuiWithNullDecoy extends WrappedGuiChat {
        @SuppressWarnings("unused")
        private GuiTextField decoy;
    }
}
