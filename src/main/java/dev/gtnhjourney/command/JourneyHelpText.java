package dev.gtnhjourney.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Chat help with one short, literal command description per line. */
public final class JourneyHelpText {

    private static final List<String> LINES = Collections.unmodifiableList(
        Arrays.asList(
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
            "/journey undo [n] - Undo the newest reversible Journey actions, including research/admin changes.",
            "/journey redo [n] - Redo the newest reversible Journey actions.",
            "/journey restore-deleted [n] - Restore deleted researched states.",
            "/journey snapshot [name] - Create an in-world Journey snapshot.",
            "/journey snapshot latest return - Return the Journey list from the latest external snapshot outside saves/.",
            "/journey snapshots - List in-world snapshots.",
            "/journey restore <id|name> - Restore an in-world snapshot.",
            "/journey backup status|now|on|off - Inspect, run or toggle world backups.",
            "/journey explosions status|on|off - Inspect or toggle global explosions.",
            "/journey explosions machines status|on|off - Inspect or toggle GregTech machine explosions only.",
            "/journey explosions default - Restore global and machine explosions to on.",
            "/journey explosions undo [n] - Undo explosion-setting changes.",
            "/journey explosions redo [n] - Redo explosion-setting changes.",
            "/journey cleanse - Remove your active negative potion effects.",
            "/journey speed machines <1|2|4|8|16> - Accelerate loaded TileEntities; capped at 16x for GT energy safety.",
            "/journey speed world <1|2|4|8|16|32|64|128> - Accelerate the complete server world.",
            "/journey speed status - Show the active speed mode and multiplier.",
            "/journey speed default - Restore MACHINES 1x.",
            "/journey speed undo [n] - Undo speed-setting changes.",
            "/journey speed redo [n] - Redo speed-setting changes.",
            "/journey death inventory status|return|undo [n]|redo [n] - Recover keepInventory death losses.",
            "/journey return death inventory - Return missing contents from the last tracked keepInventory death.",
            "/journey botania debug tool - Give the Botania Mana Debug Tool.",
            "/journey debug - Show compatibility diagnostics.",
            "/journey trace [on|off] - Toggle research tracing.",
            "/journey dump - Write a diagnostic dump.",
            "/journey hotspots [n] - Show items with many stored states.",
            "/journey debugtool - Give the Debug Researcher Tool.",
            "/journey prune-missing confirm - Remove unavailable researched states.",
            "/journey clear confirm - Clear all Journey research."));

    private JourneyHelpText() {}

    public static List<String> lines() {
        return LINES;
    }
}
