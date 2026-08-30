package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class JourneyCommandSuggestionsTest {

    @Test
    public void suggestsJourneySubcommandsWhileTyping() {
        assertEquals(Arrays.asList("trace"), JourneyCommandSuggestions.forChatText("/journey tr"));
        assertEquals(Arrays.asList("research", "rescan", "redo", "restore-deleted", "restore"),
            JourneyCommandSuggestions.forChatText("/journey re"));
        assertEquals(Arrays.asList("botania"), JourneyCommandSuggestions.forChatText("/journey bo"));
    }

    @Test
    public void suggestsFixedArguments() {
        assertEquals(Arrays.asList("on", "off"), JourneyCommandSuggestions.forChatText("/journey trace o"));
        assertEquals(Arrays.asList("confirm"), JourneyCommandSuggestions.forChatText("/journey clear c"));
        assertEquals(Arrays.asList("debug"), JourneyCommandSuggestions.forChatText("/journey botania "));
        assertEquals(Arrays.asList("tool"), JourneyCommandSuggestions.forChatText("/journey botania debug "));
    }

    @Test
    public void rootSuggestionsContainTheWholeJourneyCommandSurface() {
        List<String> suggestions = JourneyCommandSuggestions.forChatText("/journey ");
        assertTrue(suggestions.contains("rescan"));
        assertTrue(suggestions.contains("dump"));
        assertTrue(suggestions.contains("debugtool"));
        assertTrue(suggestions.contains("restore-deleted"));
        assertTrue(suggestions.contains("botania"));
    }

    @Test
    public void ignoresUnrelatedChat() {
        assertTrue(JourneyCommandSuggestions.forChatText("hello").isEmpty());
        assertTrue(JourneyCommandSuggestions.forChatText("/journeymap").isEmpty());
    }
}
