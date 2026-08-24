package dev.gtnhjourney.nei;

/** Decides how many exact server-synchronized ItemStack variants NEI should temporarily receive. */
public enum JourneyVariantScope {
    NONE,
    ALL_RESEARCHED,
    NEWEST_ONLY;

    public static JourneyVariantScope forMode(JourneyViewState.Mode mode) {
        if (mode == JourneyViewState.Mode.RESEARCHED) return ALL_RESEARCHED;
        if (mode == JourneyViewState.Mode.NEWEST) return NEWEST_ONLY;
        return NONE;
    }
}
