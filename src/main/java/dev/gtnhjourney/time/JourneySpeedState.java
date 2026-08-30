package dev.gtnhjourney.time;

/** Session-only Journey speed state. */
public final class JourneySpeedState {

    private JourneySpeedMode mode = JourneySpeedMode.MACHINES;
    private int multiplier = 1;

    public synchronized JourneySpeedMode mode() {
        return mode;
    }

    public synchronized int multiplier() {
        return multiplier;
    }

    /** Nominal accelerated tick rate. Machines mode keeps the server world itself at 20 TPS. */
    public synchronized int targetTps() {
        return multiplier * 20;
    }

    public synchronized int serverTargetTps() {
        return mode == JourneySpeedMode.WORLD ? multiplier * 20 : 20;
    }

    public synchronized boolean trySet(JourneySpeedMode newMode, int value) {
        if (newMode == null || !isAllowedMultiplier(value)) return false;
        mode = newMode;
        multiplier = value;
        return true;
    }

    public synchronized boolean trySetMultiplier(int value) {
        return trySet(mode, value);
    }

    public synchronized void reset() {
        mode = JourneySpeedMode.MACHINES;
        multiplier = 1;
    }

    public static boolean isAllowedMultiplier(int value) {
        return value == 1 || value == 2 || value == 4 || value == 8 || value == 16 || value == 32 || value == 64
            || value == 128;
    }
}
