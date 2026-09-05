package dev.gtnhjourney.nei;

/** Pure page restoration after NEI's updateItemList may internally reset the grid to page zero. */
final class JourneyPageRetentionPolicy {

    private JourneyPageRetentionPolicy() {}

    static int pageAfterRefresh(int previousZeroBasedPage, int numPages, boolean resetRequested) {
        if (resetRequested) return 0;
        int previous = Math.max(0, previousZeroBasedPage);
        int last = Math.max(0, numPages - 1);
        return Math.min(previous, last);
    }
}
