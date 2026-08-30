package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ServerTickOverrunGuardTest {

    @Test
    public void legacySpeedsKeepExactScheduledSubtraction() {
        assertEquals(6L, ServerTickOverrunGuard.subtractionMillis(8, 6L, 40L));
        assertEquals(25L, ServerTickOverrunGuard.subtractionMillis(2, 25L, 100L));
    }

    @Test
    public void highSpeedsDrainPreviousUnsustainableBatchCost() {
        assertEquals(3L, ServerTickOverrunGuard.subtractionMillis(16, 3L, 2L));
        assertEquals(12L, ServerTickOverrunGuard.subtractionMillis(16, 3L, 12L));
        assertEquals(8L, ServerTickOverrunGuard.subtractionMillis(64, 1L, 8L));
        assertEquals(30L, ServerTickOverrunGuard.subtractionMillis(128, 2L, 30L));
    }

    @Test
    public void pathologicalStallIsCappedAtVanillaTwoSecondLagWindow() {
        assertEquals(2000L, ServerTickOverrunGuard.subtractionMillis(128, 1L, 9000L));
    }
}
