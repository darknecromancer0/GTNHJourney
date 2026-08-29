package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class JourneySpeedCommandPolicyTest {

    @Test
    public void statusIsReadableAndMutationsRequireAdmin() {
        assertFalse(JourneySpeedCommandPolicy.requiresAdmin(new String[] { "speed" }));
        assertFalse(JourneySpeedCommandPolicy.requiresAdmin(new String[] { "speed", "status" }));
        assertTrue(JourneySpeedCommandPolicy.requiresAdmin(new String[] { "speed", "1" }));
        assertTrue(JourneySpeedCommandPolicy.requiresAdmin(new String[] { "speed", "2" }));
        assertTrue(JourneySpeedCommandPolicy.requiresAdmin(new String[] { "speed", "4" }));
        assertTrue(JourneySpeedCommandPolicy.requiresAdmin(new String[] { "speed", "8" }));
    }

    @Test
    public void parserAcceptsOnlySupportedMultipliers() {
        assertEquals(Integer.valueOf(1), JourneySpeedCommandPolicy.parseMultiplier("1"));
        assertEquals(Integer.valueOf(2), JourneySpeedCommandPolicy.parseMultiplier("2"));
        assertEquals(Integer.valueOf(4), JourneySpeedCommandPolicy.parseMultiplier("4"));
        assertEquals(Integer.valueOf(8), JourneySpeedCommandPolicy.parseMultiplier("8"));
        assertEquals(null, JourneySpeedCommandPolicy.parseMultiplier("3"));
        assertEquals(null, JourneySpeedCommandPolicy.parseMultiplier("banana"));
        assertEquals(null, JourneySpeedCommandPolicy.parseMultiplier(null));
    }

    @Test
    public void liveSuggestionsExposeOnlyTheSpeedSurface() {
        assertEquals(
            Arrays.asList("1", "2", "4", "8", "status"),
            JourneyCommandSuggestions.forChatText("/journey speed "));
        assertEquals(Arrays.asList("status"), JourneyCommandSuggestions.forChatText("/journey speed st"));
    }
}
