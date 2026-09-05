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
        "backup",
        "explosions",
        "cleanse",
        "speed",
        "botania",
        "debug",
        "trace",
        "dump",
        "hotspots",
        "debugtool",
        "prune-missing",
        "clear",
        "return",
        "death"));

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
            if ("snapshot".equals(action)) return matching(args[1], Collections.singletonList("latest"));
            if ("backup".equals(action)) return matching(args[1], Arrays.asList("status", "now", "on", "off"));
            if ("explosions".equals(action)) {
                return matching(args[1], Arrays.asList("status", "on", "off", "default", "undo", "redo", "machines"));
            }
            if ("speed".equals(action)) {
                return matching(args[1], Arrays.asList("status", "default", "undo", "redo", "machines", "world", "1", "2", "4", "8", "16"));
            }
            if ("return".equals(action)) return matching(args[1], Collections.singletonList("death"));
            if ("death".equals(action)) return matching(args[1], Collections.singletonList("inventory"));
            if ("botania".equals(action)) return matching(args[1], Collections.singletonList("debug"));
            if ("clear".equals(action) || "prune-missing".equals(action)) {
                return matching(args[1], Collections.singletonList("confirm"));
            }
        }
        if (args.length == 3) {
            if ("snapshot".equalsIgnoreCase(args[0]) && "latest".equalsIgnoreCase(args[1])) {
                return matching(args[2], Collections.singletonList("return"));
            }
            if ("explosions".equalsIgnoreCase(args[0]) && "machines".equalsIgnoreCase(args[1])) {
                return matching(args[2], Arrays.asList("status", "on", "off"));
            }
            if ("speed".equalsIgnoreCase(args[0]) && "machines".equalsIgnoreCase(args[1])) {
                return matching(args[2], Arrays.asList("1", "2", "4", "8", "16"));
            }
            if ("speed".equalsIgnoreCase(args[0]) && "world".equalsIgnoreCase(args[1])) {
                return matching(args[2], Arrays.asList("1", "2", "4", "8", "16", "32", "64", "128"));
            }
            if ("return".equalsIgnoreCase(args[0]) && "death".equalsIgnoreCase(args[1])) {
                return matching(args[2], Collections.singletonList("inventory"));
            }
            if ("death".equalsIgnoreCase(args[0]) && "inventory".equalsIgnoreCase(args[1])) {
                return matching(args[2], Arrays.asList("status", "return", "undo", "redo"));
            }
            if ("botania".equalsIgnoreCase(args[0]) && "debug".equalsIgnoreCase(args[1])) {
                return matching(args[2], Collections.singletonList("tool"));
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
