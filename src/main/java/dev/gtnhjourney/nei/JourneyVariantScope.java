package dev.gtnhjourney.nei;

import dev.gtnhjourney.research.ResearchKey;

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

    /** Native BASE item/meta entries already exist in NEI; only exact NBT states need temporary Journey variants. */
    public static boolean shouldInjectVariant(ResearchKey key) {
        return key != null && !key.getCanonicalNbt().isEmpty();
    }
}
