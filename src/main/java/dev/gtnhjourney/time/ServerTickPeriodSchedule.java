package dev.gtnhjourney.time;

/** Exact-average millisecond pacing cycles plus whole-server tick bursts for high Journey speeds. */
public final class ServerTickPeriodSchedule {

    private static final long[] ONE = { 50L };
    private static final long[] TWO = { 25L };
    private static final long[] FOUR = { 12L, 13L };
    private static final long[] EIGHT = { 6L, 6L, 6L, 7L };
    private static final long[] SIXTEEN = { 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L };
    private static final long[] THIRTY_TWO = { 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 2L, 2L };

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

    public static int fullTicksPerOuterTick(int multiplier) {
        switch (multiplier) {
            case 64:
                return 2;
            case 128:
                return 4;
            default:
                return 1;
        }
    }

    public static double effectiveAverageMillisPerFullTick(int multiplier) {
        return averageMillis(multiplier) / (double) fullTicksPerOuterTick(multiplier);
    }

    private static long[] cycle(int multiplier) {
        switch (multiplier) {
            case 2:
                return TWO;
            case 4:
                return FOUR;
            case 8:
                return EIGHT;
            case 16:
                return SIXTEEN;
            case 32:
            case 64:
            case 128:
                return THIRTY_TWO;
            case 1:
            default:
                return ONE;
        }
    }
}
