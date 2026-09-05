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
            "/journey snapshot [name] - Create an in-world Journey snapshot.",
            "/journey snapshot latest return - Return the Journey list from the latest external snapshot outside saves/.",
            "/journey snapshots - List in-world snapshots.",
            "/journey restore <id|name> - Restore an in-world snapshot.",
            "/journey backup status|now|on|off - Inspect, run or toggle world backups.",
            "/journey explosions status|on|off - Inspect or toggle global explosions.",
            "/journey cleanse - Remove your active negative potion effects.",
            "/journey speed machines <1|2|4|8|16> - Accelerate loaded TileEntities; capped at 16x for GT energy safety.",
            "/journey speed world <1|2|4|8|16|32|64|128> - Accelerate the complete server world.",
            "/journey speed status - Show the active speed mode and multiplier.",
            "/journey death inventory status|return|undo [n]|redo [n] - Recover keepInventory death losses.",
            "/journey return death inventory - Return missing contents from the last tracked keepInventory death.",
            "/journey botania debug tool - Give the Botania Mana Debug Tool.",
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
