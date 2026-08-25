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

    /** Backwards-compatible policy: assume a blank-NBT BASE already exists natively in NEI. */
    public static boolean shouldInjectVariant(ResearchKey key) {
        return shouldInjectVariant(key, true);
    }

    /** Exact NBT states always need injection; blank-NBT states need it only when NEI lacks that exact item/meta state. */
    public static boolean shouldInjectVariant(ResearchKey key, boolean nativeExactPresent) {
        return key != null && (!key.getCanonicalNbt().isEmpty() || !nativeExactPresent);
    }
}
