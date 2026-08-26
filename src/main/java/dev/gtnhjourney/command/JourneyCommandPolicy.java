package dev.gtnhjourney.command;

/** Pure parsing/clamp rules and recovery-facing text for Journey commands. */
public final class JourneyCommandPolicy {

    private JourneyCommandPolicy() {}

    public static int parseUndoRedoCount(String raw) {
        return parseCount(raw, 100);
    }

    public static int parseRestoreDeletedCount(String raw) {
        return parseCount(raw, 1000);
    }

    public static String pruneMissingResult(int removed) {
        return "Pruned " + Math.max(0, removed)
            + " unavailable states. Undo is retained, but restore is blocked until those items exist again.";
    }

    private static int parseCount(String raw, int maximum) {
        if (raw == null || raw.trim().isEmpty()) return 1;
        try {
            return Math.max(1, Math.min(maximum, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
