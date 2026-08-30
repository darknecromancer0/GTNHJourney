package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CommandJourneyTabCompletionTest {

    private static final List<String> ALL_COMMANDS = Arrays.asList(
        "help",
        "count",
        "stats",
        "inspect",
        "research",
        "rescan",
        "list",
        "newest",
        "get",
        "forget",
        "undo",
        "redo",
        "restore-deleted",
        "snapshot",
        "snapshots",
        "restore",
        "debug",
        "trace",
        "dump",
        "hotspots",
        "debugtool",
        "prune-missing",
        "clear",
        "backup",
        "explosions",
        "cleanse",
        "speed",
        "botania");

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
        assertEquals(
            Arrays.asList("machines", "world", "1", "2", "4", "8", "16", "32", "64", "128", "status"),
            command.addTabCompletionOptions(null, new String[] { "speed", "" }));
        assertEquals(
            Arrays.asList("1", "2", "4", "8", "16", "32", "64", "128"),
            command.addTabCompletionOptions(null, new String[] { "speed", "machines", "" }));
        assertEquals(
            Arrays.asList("1", "2", "4", "8", "16", "32", "64", "128"),
            command.addTabCompletionOptions(null, new String[] { "speed", "world", "" }));
        assertEquals(
            Arrays.asList("debug"),
            command.addTabCompletionOptions(null, new String[] { "botania", "" }));
        assertEquals(
            Arrays.asList("tool"),
            command.addTabCompletionOptions(null, new String[] { "botania", "debug", "" }));
    }

    @Test
    public void rootCompletionIncludesEveryJourneyCommand() {
        List<?> options = command.addTabCompletionOptions(null, new String[] { "" });

        assertNotNull(options);
        for (String expected : ALL_COMMANDS) {
            assertTrue(options.contains(expected), "missing completion for " + expected);
        }
    }
}
