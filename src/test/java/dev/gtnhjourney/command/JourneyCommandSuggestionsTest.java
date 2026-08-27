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
    }

    @Test
    public void suggestsFixedArguments() {
        assertEquals(Arrays.asList("on", "off"), JourneyCommandSuggestions.forChatText("/journey trace o"));
        assertEquals(Arrays.asList("confirm"), JourneyCommandSuggestions.forChatText("/journey clear c"));
    }

    @Test
    public void rootSuggestionsContainTheWholeJourneyCommandSurface() {
        List<String> suggestions = JourneyCommandSuggestions.forChatText("/journey ");
        assertTrue(suggestions.contains("rescan"));
        assertTrue(suggestions.contains("dump"));
        assertTrue(suggestions.contains("debugtool"));
        assertTrue(suggestions.contains("restore-deleted"));
    }

    @Test
    public void ignoresUnrelatedChat() {
        assertTrue(JourneyCommandSuggestions.forChatText("hello").isEmpty());
        assertTrue(JourneyCommandSuggestions.forChatText("/journeymap").isEmpty());
    }
}
