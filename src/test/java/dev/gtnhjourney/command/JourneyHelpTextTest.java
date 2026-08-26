package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class JourneyHelpTextTest {

    @Test
    public void helpListsEveryCommandOnItsOwnShortLine() {
        List<String> expected = Arrays.asList(
            "GTNH Journey commands:",
            "/journey help - Show this help.",
            "/journey count - Show researched state count.",
            "/journey stats - Show research statistics.",
            "/journey inspect - Inspect the held item's Journey identity.",
            "/journey research - Research or refresh the held item.",
            "/journey rescan - Rescan your real inventory.",
            "/journey list [page] - List researched states.",
            "/journey newest [n] - List newest researched states.",
            "/journey get <index> [amount] - Retrieve a researched state.",
            "/journey forget <index> - Forget a researched state.",
            "/journey undo [n] - Undo Journey changes.",
            "/journey redo [n] - Redo Journey changes.",
            "/journey restore-deleted [n] - Restore deleted states.",
            "/journey snapshot [name] - Create a snapshot.",
            "/journey snapshots - List snapshots.",
            "/journey restore <id|name> - Restore a snapshot.",
            "/journey debug - Show compatibility diagnostics.",
            "/journey trace [on|off] - Toggle research tracing.",
            "/journey dump - Write a diagnostic dump.",
            "/journey hotspots [n] - Show items with many stored states.",
            "/journey debugtool - Give the Debug Researcher Tool.",
            "/journey prune-missing confirm - Remove unavailable researched states.",
            "/journey clear confirm - Clear all Journey research.");

        assertEquals(expected, JourneyHelpText.lines());
    }
}
