package dev.gtnhjourney.time;

/**
 * Safety boundary for acceleration modes.
 *
 * <p>Machine mode directly adds TileEntity updates and therefore cannot safely run past 16x on GTNH: at higher
 * multipliers the per-tick work budget can advance energy producers/buffers farther than downstream consumers before
 * the pass completes. World mode accelerates the whole server cadence instead, so the ordinary allowed range remains
 * available there.</p>
 */
public final class JourneySpeedSafetyPolicy {

    public static final int MAX_SAFE_MACHINE_MULTIPLIER = 16;

    private JourneySpeedSafetyPolicy() {}

    public static boolean isSafe(JourneySpeedMode mode, int multiplier) {
        if (mode == null || !JourneySpeedState.isAllowedMultiplier(multiplier)) return false;
        return mode != JourneySpeedMode.MACHINES || multiplier <= MAX_SAFE_MACHINE_MULTIPLIER;
    }
}
