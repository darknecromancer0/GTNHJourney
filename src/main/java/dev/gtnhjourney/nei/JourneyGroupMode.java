package dev.gtnhjourney.nei;

/** Optional grouping dimension for Journey-owned NEI views. */
public enum JourneyGroupMode {
    NONE("-", "None"),
    NATIVE("N", "Native NEI family"),
    MOD("M", "Mod"),
    TYPE("T", "Type"),
    KIND("K", "Similar kind");

    private final String abbreviation;
    private final String label;

    JourneyGroupMode(String abbreviation, String label) {
        this.abbreviation = abbreviation;
        this.label = label;
    }

    public String abbreviation() { return abbreviation; }
    public String label() { return label; }
}
