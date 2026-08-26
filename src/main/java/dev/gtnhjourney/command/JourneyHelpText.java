package dev.gtnhjourney.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Compact chat help. Commands are grouped so the full reference does not flood the player's screen. */
public final class JourneyHelpText {

    private static final List<String> LINES = Collections.unmodifiableList(
        Arrays.asList(
            "GTNH Journey commands:",
            "/journey count | /journey stats | /journey inspect - research summary and held item diagnostics.",
            "/journey research | /journey rescan - research the held item or rescan your real inventory.",
            "/journey list [page] | /journey newest [n] | /journey get <index> [amount] - browse and retrieve research.",
            "/journey forget <index> | /journey undo [n] | /journey redo [n] | /journey restore-deleted [n] - delete and recover states.",
            "/journey snapshot [name] | /journey snapshots | /journey restore <id|name> - create, list and restore snapshots.",
            "/journey debug | /journey trace [on|off] | /journey dump | /journey hotspots [n] - diagnostics and troubleshooting.",
            "/journey debugtool | /journey prune-missing confirm | /journey clear confirm - migration and destructive tools."));

    private JourneyHelpText() {}

    public static List<String> lines() {
        return LINES;
    }
}
