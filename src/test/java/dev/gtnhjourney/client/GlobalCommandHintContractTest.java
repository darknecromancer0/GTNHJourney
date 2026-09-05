package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class GlobalCommandHintContractTest {

    @Test
    public void overlayUsesServerAuthoritativeSuggestionsInsteadOfJourneyOnlyCatalog() throws IOException {
        String overlay = read("src/main/java/dev/gtnhjourney/client/JourneyCommandHintOverlay.java");
        assertTrue(overlay.contains("ClientCommandSuggestionState"));
        assertTrue(overlay.contains("requestForInput"));
        assertFalse(overlay.contains("JourneyCommandSuggestions.forChatText"));
    }

    @Test
    public void guiChatMixinOwnsArrowSelectionAndTabAcceptanceOnlyWhenPopupIsActive() throws IOException {
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/GuiChatCommandHintMixin.java");
        String config = read("src/main/resources/mixins.gtnhjourney.json");
        assertTrue(mixin.contains("@Mixin(GuiChat.class)"));
        assertTrue(mixin.contains("method = \"keyTyped\""));
        assertTrue(mixin.contains("CommandHintKeyHandler.handle"));
        assertTrue(mixin.contains("ci.cancel()"));
        assertTrue(config.contains("\"GuiChatCommandHintMixin\""));
    }

    @Test
    public void completionRequestIsResolvedOnServerCommandManagerForAllSlashCommands() throws IOException {
        String queue = read("src/main/java/dev/gtnhjourney/network/CommandSuggestionRequestQueue.java");
        assertTrue(queue.contains("getCommandManager()"));
        assertTrue(queue.contains("getPossibleCommands(player, commandPrefix)"));
        assertFalse(queue.contains("JourneyCommandSuggestions"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
