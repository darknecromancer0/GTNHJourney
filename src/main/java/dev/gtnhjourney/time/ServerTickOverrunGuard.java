package dev.gtnhjourney.time;

/** Prevents high Journey speeds from turning unsustainable tick cost into an ever-growing vanilla catch-up backlog. */
public final class ServerTickOverrunGuard {

    private static final int GUARDED_MIN_MULTIPLIER = 16;
    private static final long VANILLA_LAG_WINDOW_MILLIS = 2000L;

    private ServerTickOverrunGuard() {}

    public static long subtractionMillis(int multiplier, long scheduledPeriodMillis, long previousOuterTickCostMillis) {
        long scheduled = Math.max(1L, scheduledPeriodMillis);
        if (multiplier < GUARDED_MIN_MULTIPLIER) return scheduled;

        long previousCost = Math.max(0L, previousOuterTickCostMillis);
        if (previousCost <= scheduled) return scheduled;
        return Math.min(VANILLA_LAG_WINDOW_MILLIS, previousCost);
    }
}
