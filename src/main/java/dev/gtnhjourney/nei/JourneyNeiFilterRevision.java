package dev.gtnhjourney.nei;

/** Client-side revision bumped whenever NEI restarts its native item-filter task. */
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
