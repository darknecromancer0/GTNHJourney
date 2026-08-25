package dev.gtnhjourney.command;

/** Pure parsing/clamp rules for recovery commands so malformed counts never expand a destructive operation. */
public final class JourneyCommandPolicy {

    private JourneyCommandPolicy() {}

    public static int parseUndoRedoCount(String raw) {
        return parseCount(raw, 100);
    }

    public static int parseRestoreDeletedCount(String raw) {
        return parseCount(raw, 1000);
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
