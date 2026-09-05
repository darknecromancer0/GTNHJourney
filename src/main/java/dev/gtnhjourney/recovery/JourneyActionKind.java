package dev.gtnhjourney.recovery;

/** Non-research Journey mutations that participate in the persistent undo/redo journal. */
public enum JourneyActionKind {
    SPEED,
    EXPLOSIONS,
    FAVOURITE,
    DEATH_INVENTORY_RETURN;

    public static JourneyActionKind parse(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
