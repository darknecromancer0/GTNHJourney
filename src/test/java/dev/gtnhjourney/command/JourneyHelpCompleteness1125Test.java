package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneyHelpCompleteness1125Test {

    @Test
    public void helpMentionsEveryNewAdministrativeBranch() {
        String joined = String.join("\n", JourneyHelpText.lines());
        assertTrue(joined.contains("/journey undo [n]"));
        assertTrue(joined.contains("/journey redo [n]"));
        assertTrue(joined.contains("/journey speed default"));
        assertTrue(joined.contains("/journey speed undo [n]"));
        assertTrue(joined.contains("/journey speed redo [n]"));
        assertTrue(joined.contains("/journey explosions default"));
        assertTrue(joined.contains("/journey explosions undo [n]"));
        assertTrue(joined.contains("/journey explosions redo [n]"));
        assertTrue(joined.contains("/journey explosions machines status|on|off"));
        assertTrue(joined.contains("/journey death inventory status|return|undo [n]|redo [n]"));
        assertTrue(joined.contains("/journey snapshot latest return"));
    }
}
