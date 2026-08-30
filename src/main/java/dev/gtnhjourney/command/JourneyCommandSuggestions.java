package dev.gtnhjourney.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Shared completion surface for vanilla Tab completion and the client-side live chat hint overlay. */
public final class JourneyCommandSuggestions {

    private static final String ROOT = "/journey";
    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList(
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
        "speed"));

    private JourneyCommandSuggestions() {}

    public static List<String> forChatText(String text) {
        if (text == null || !text.startsWith(ROOT)) return Collections.emptyList();
        if (text.length() > ROOT.length() && text.charAt(ROOT.length()) != ' ') return Collections.emptyList();
        if (text.length() == ROOT.length()) return SUBCOMMANDS;

        String tail = text.substring(ROOT.length() + 1);
        return forArgs(tail.split(" ", -1));
    }

    public static List<String> forArgs(String[] args) {
        if (args == null || args.length == 0) return SUBCOMMANDS;
        if (args.length == 1) return matching(args[0], SUBCOMMANDS);
        if (args.length == 2) {
            String action = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
            if ("trace".equals(action)) return matching(args[1], Arrays.asList("on", "off"));
            if ("speed".equals(action)) {
                return matching(args[1], Arrays.asList("1", "2", "4", "8", "16", "32", "64", "128", "status"));
            }
            if ("clear".equals(action) || "prune-missing".equals(action)) {
                return matching(args[1], Collections.singletonList("confirm"));
            }
        }
        return Collections.emptyList();
    }

    public static String[] subcommands() {
        return SUBCOMMANDS.toArray(new String[SUBCOMMANDS.size()]);
    }

    private static List<String> matching(String prefix, List<String> candidates) {
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        for (String candidate : candidates) {
            if (candidate.startsWith(needle)) out.add(candidate);
        }
        return Collections.unmodifiableList(out);
    }
}
