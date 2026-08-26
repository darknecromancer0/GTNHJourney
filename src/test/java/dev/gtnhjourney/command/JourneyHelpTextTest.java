package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class JourneyHelpTextTest {

    @Test
    public void helpIsShortDescriptiveAndIncludesHeldResearch() {
        List<String> lines = JourneyHelpText.lines();

        assertTrue(lines.size() <= 9, "help should remain compact in chat");
        assertContains(lines, "/journey research");
        assertContains(lines, "held item");
        assertContains(lines, "/journey rescan");
        assertContains(lines, "/journey undo");
        assertContains(lines, "/journey snapshot");
        assertContains(lines, "/journey dump");
        assertContains(lines, "delete");
    }

    private static void assertContains(List<String> lines, String expected) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Missing help text: " + expected + " in " + lines);
    }
}
