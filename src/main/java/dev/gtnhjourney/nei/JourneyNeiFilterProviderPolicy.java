package dev.gtnhjourney.nei;

/** Separates Journey-compatible query filters from NEI's global item-visibility masks. */
final class JourneyNeiFilterProviderPolicy {

    private static final String SUBSET_WIDGET = "codechicken.nei.SubsetWidget";

    private JourneyNeiFilterProviderPolicy() {}

    static boolean shouldApply(String providerClassName, boolean searchField, boolean searchVisible) {
        if (searchField) return searchVisible;
        return !SUBSET_WIDGET.equals(providerClassName);
    }
}
