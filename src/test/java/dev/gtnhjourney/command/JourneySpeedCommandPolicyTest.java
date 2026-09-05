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
        for (String value : new String[] { "1", "2", "4", "8", "16", "32", "64", "128" }) {
            assertTrue(JourneySpeedCommandPolicy.requiresAdmin(new String[] { "speed", value }));
        }
    }

    @Test
    public void parserAcceptsTheSharedMultiplierVocabulary() {
        for (int value : new int[] { 1, 2, 4, 8, 16, 32, 64, 128 }) {
            assertEquals(Integer.valueOf(value), JourneySpeedCommandPolicy.parseMultiplier(Integer.toString(value)));
        }
        assertEquals(null, JourneySpeedCommandPolicy.parseMultiplier("3"));
        assertEquals(null, JourneySpeedCommandPolicy.parseMultiplier("256"));
        assertEquals(null, JourneySpeedCommandPolicy.parseMultiplier("banana"));
        assertEquals(null, JourneySpeedCommandPolicy.parseMultiplier(null));
    }

    @Test
    public void liveSuggestionsSeparateSafeMachinesFromFullWorldRange() {
        assertEquals(
            Arrays.asList("status", "default", "undo", "redo", "machines", "world", "1", "2", "4", "8", "16"),
            JourneyCommandSuggestions.forChatText("/journey speed "));
        assertEquals(
            Arrays.asList("1", "16"),
            JourneyCommandSuggestions.forChatText("/journey speed 1"));
        assertEquals(Arrays.asList("status"), JourneyCommandSuggestions.forChatText("/journey speed st"));
        assertEquals(
            Arrays.asList("1", "2", "4", "8", "16"),
            JourneyCommandSuggestions.forChatText("/journey speed machines "));
        assertEquals(
            Arrays.asList("1", "2", "4", "8", "16", "32", "64", "128"),
            JourneyCommandSuggestions.forChatText("/journey speed world "));
    }
}
