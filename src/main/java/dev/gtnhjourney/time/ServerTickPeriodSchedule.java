package dev.gtnhjourney.time;

/** Exact-average millisecond pacing cycles for 20/40/80/160 TPS. */
public final class ServerTickPeriodSchedule {

    private static final long[] ONE = { 50L };
    private static final long[] TWO = { 25L };
    private static final long[] FOUR = { 12L, 13L };
    private static final long[] EIGHT = { 6L, 6L, 6L, 7L };

    private ServerTickPeriodSchedule() {}

    public static long[] cycleMillis(int multiplier) {
        long[] cycle = cycle(multiplier);
        return cycle.clone();
    }

    public static long periodMillis(int multiplier, int phase) {
        long[] cycle = cycle(multiplier);
        int index = phase % cycle.length;
        if (index < 0) index += cycle.length;
        return cycle[index];
    }

    public static int nextPhase(int multiplier, int phase) {
        long[] cycle = cycle(multiplier);
        int next = phase + 1;
        if (next < 0 || next >= cycle.length) return 0;
        return next;
    }

    public static double averageMillis(int multiplier) {
        long[] cycle = cycle(multiplier);
        long total = 0L;
        for (long period : cycle) total += period;
        return (double) total / (double) cycle.length;
    }

    private static long[] cycle(int multiplier) {
        switch (multiplier) {
            case 2:
                return TWO;
            case 4:
                return FOUR;
            case 8:
                return EIGHT;
            case 1:
            default:
                return ONE;
        }
    }
}
