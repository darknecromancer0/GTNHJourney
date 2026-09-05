package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JourneySpeedSafetyPolicyTest {

    @Test
    void machineModeHardCapsAtSixteenToKeepGtPowerAndConsumersInLockstep() {
        assertEquals(16, JourneySpeedSafetyPolicy.MAX_SAFE_MACHINE_MULTIPLIER);
        assertTrue(JourneySpeedSafetyPolicy.isSafe(JourneySpeedMode.MACHINES, 16));
        assertFalse(JourneySpeedSafetyPolicy.isSafe(JourneySpeedMode.MACHINES, 32));
        assertFalse(JourneySpeedSafetyPolicy.isSafe(JourneySpeedMode.MACHINES, 128));
        assertEquals(16, JourneySpeedSafetyPolicy.effectiveMultiplier(JourneySpeedMode.MACHINES, 128));
    }

    @Test
    void worldModeKeepsItsExistingMultiplierRange() {
        assertTrue(JourneySpeedSafetyPolicy.isSafe(JourneySpeedMode.WORLD, 32));
        assertTrue(JourneySpeedSafetyPolicy.isSafe(JourneySpeedMode.WORLD, 128));
        assertEquals(128, JourneySpeedSafetyPolicy.effectiveMultiplier(JourneySpeedMode.WORLD, 128));
    }
}
