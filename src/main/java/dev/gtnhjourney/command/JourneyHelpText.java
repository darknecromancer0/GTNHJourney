package dev.gtnhjourney.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Compact chat help. Commands are grouped so the full reference does not flood the player's screen. */
public final class JourneyHelpText {

    private static final List<String> LINES = Collections.unmodifiableList(
        Arrays.asList(
            "GTNH Journey commands:",
            "/journey count | stats | inspect - research summary and held-item diagnostics.",
            "/journey research | rescan - research the held item or rescan your real inventory.",
            "/journey list [page] | newest [n] | get <index> [amount] - browse and retrieve research.",
            "/journey forget <index> | undo [n] | redo [n] | restore-deleted [n] - delete and recover states.",
            "/journey snapshot [name] | snapshots | restore <id|name> - create, list and restore snapshots.",
            "/journey debug | trace [on|off] | dump | hotspots [n] - diagnostics and troubleshooting.",
            "/journey debugtool | prune-missing confirm | clear confirm - migration and destructive tools."));

    private JourneyHelpText() {}

    public static List<String> lines() {
        return LINES;
    }
}
