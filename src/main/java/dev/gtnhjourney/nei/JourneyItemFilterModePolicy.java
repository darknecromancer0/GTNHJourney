package dev.gtnhjourney.nei;

/** Journey's registered NEI provider must not recursively filter a panel already owned by Journey. */
final class JourneyItemFilterModePolicy {

    private JourneyItemFilterModePolicy() {}

    static boolean allowThrough(JourneyViewState.Mode mode) {
        return true;
    }
}
