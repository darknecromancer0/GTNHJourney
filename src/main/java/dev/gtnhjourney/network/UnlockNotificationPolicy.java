package dev.gtnhjourney.network;

/** One observed logical item may unlock several semantic endpoints, but should notify only once. */
public final class UnlockNotificationPolicy {

    private UnlockNotificationPolicy() {}

    public static boolean shouldNotify(int newlyUnlockedEndpoints) {
        return newlyUnlockedEndpoints > 0;
    }
}
