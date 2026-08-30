package dev.gtnhjourney.time;

/** Session-only Journey server speed state. */
public final class JourneySpeedState {

    private int multiplier = 1;

    public synchronized int multiplier() {
        return multiplier;
    }

    public synchronized int targetTps() {
        return multiplier * 20;
    }

    public synchronized boolean trySetMultiplier(int value) {
        if (!isAllowedMultiplier(value)) return false;
        multiplier = value;
        return true;
    }

    public synchronized void reset() {
        multiplier = 1;
    }

    public static boolean isAllowedMultiplier(int value) {
        return value == 1 || value == 2 || value == 4 || value == 8 || value == 16 || value == 32 || value == 64
            || value == 128;
    }
}
