package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CommandJourneyTabCompletionTest {

    private final CommandJourney command = new CommandJourney();

    @Test
    public void completesSubcommandsByPrefix() {
        List<?> options = command.addTabCompletionOptions(null, new String[] { "tr" });

        assertEquals(Arrays.asList("trace"), options);
    }

    @Test
    public void completesFixedArguments() {
        assertEquals(
            Arrays.asList("on", "off"),
            command.addTabCompletionOptions(null, new String[] { "trace", "o" }));
        assertEquals(
            Arrays.asList("confirm"),
            command.addTabCompletionOptions(null, new String[] { "clear", "c" }));
        assertEquals(
            Arrays.asList("confirm"),
            command.addTabCompletionOptions(null, new String[] { "prune-missing", "c" }));
    }

    @Test
    public void rootCompletionIncludesEveryJourneyCommand() {
        List<?> options = command.addTabCompletionOptions(null, new String[] { "" });

        for (String expected : JourneyCommandCompletion.subcommands()) {
            assertTrue(options.contains(expected), "missing completion for " + expected);
        }
    }
}
