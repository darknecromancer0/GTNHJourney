package dev.gtnhjourney.nei;

/** Pure policy used by the NEI creative GUI integration. */
public final class JourneyCreativeVisibilityPolicy {

    private JourneyCreativeVisibilityPolicy() {}

    public static boolean forceItemSection(boolean creativeScreen) {
        return creativeScreen;
    }
}
