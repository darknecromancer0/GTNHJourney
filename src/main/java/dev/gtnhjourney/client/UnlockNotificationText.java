package dev.gtnhjourney.client;

/** Pure text formatting for human-facing research unlock notifications. */
public final class UnlockNotificationText {

    private UnlockNotificationText() {}

    public static String format(String displayName) {
        String safe = displayName == null ? "item" : displayName.trim();
        if (safe.isEmpty()) safe = "item";
        return "Unlocked: " + safe;
    }
}
