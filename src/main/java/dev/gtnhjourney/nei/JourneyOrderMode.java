package dev.gtnhjourney.nei;

/** Optional ordering dimension for Journey-owned NEI views. */
public enum JourneyOrderMode {
    NONE("-", "None"),
    UNLOCK("U", "Unlock order"),
    ISSUED("I", "Issued order"),
    ALPHABETICAL("A", "Alphabetical"),
    FAVOURITE_ADDED("F+", "Favourite added");

    private final String abbreviation;
    private final String label;

    JourneyOrderMode(String abbreviation, String label) {
        this.abbreviation = abbreviation;
        this.label = label;
    }

    public String abbreviation() { return abbreviation; }
    public String label() { return label; }
}
