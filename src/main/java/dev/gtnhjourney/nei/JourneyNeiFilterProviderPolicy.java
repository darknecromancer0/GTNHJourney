package dev.gtnhjourney.nei;

/** Lets Journey consume the same native NEI filter providers as the normal item panel. */
final class JourneyNeiFilterProviderPolicy {

    private JourneyNeiFilterProviderPolicy() {}

    static boolean shouldApply(String providerClassName, boolean searchField, boolean searchVisible) {
        if (searchField) return searchVisible;
        return true;
    }
}
