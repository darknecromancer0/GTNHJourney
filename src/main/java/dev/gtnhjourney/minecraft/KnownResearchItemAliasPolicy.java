package dev.gtnhjourney.minecraft;

/** Exact registry aliases where an internal/unusable item form should resolve to the normal obtainable form. */
public final class KnownResearchItemAliasPolicy {

    private static final String FERTILIZED_DIRT_TILLED = "RandomThings:fertilizedDirt_tilled";
    private static final String FERTILIZED_DIRT = "RandomThings:fertilizedDirt";

    private KnownResearchItemAliasPolicy() {}

    public static String canonicalItemId(String itemId) {
        if (FERTILIZED_DIRT_TILLED.equals(itemId)) return FERTILIZED_DIRT;
        return itemId;
    }
}
