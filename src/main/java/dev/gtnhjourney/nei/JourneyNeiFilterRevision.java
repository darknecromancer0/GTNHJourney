package dev.gtnhjourney.nei;

/** Client-side revision bumped only when a completed NEI/Journey filter result is staged for client-thread publication. */
public final class JourneyNeiFilterRevision {

    private static long revision;

    private JourneyNeiFilterRevision() {}

    public static synchronized void invalidate() {
        revision++;
    }

    public static synchronized long revision() {
        return revision;
    }

    public static synchronized void reset() {
        revision = 0L;
    }
}
