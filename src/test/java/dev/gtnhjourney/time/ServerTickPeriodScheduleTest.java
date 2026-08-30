package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ServerTickPeriodScheduleTest {

    @Test
    public void periodsRepresentExactRequestedOuterCadence() {
        assertArrayEquals(new long[] { 50L }, ServerTickPeriodSchedule.cycleMillis(1));
        assertArrayEquals(new long[] { 25L }, ServerTickPeriodSchedule.cycleMillis(2));
        assertArrayEquals(new long[] { 12L, 13L }, ServerTickPeriodSchedule.cycleMillis(4));
        assertArrayEquals(new long[] { 6L, 6L, 6L, 7L }, ServerTickPeriodSchedule.cycleMillis(8));
        assertArrayEquals(new long[] { 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L }, ServerTickPeriodSchedule.cycleMillis(16));
        assertArrayEquals(
            new long[] { 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 2L, 2L },
            ServerTickPeriodSchedule.cycleMillis(32));

        assertEquals(50.0D, ServerTickPeriodSchedule.averageMillis(1), 0.0001D);
        assertEquals(25.0D, ServerTickPeriodSchedule.averageMillis(2), 0.0001D);
        assertEquals(12.5D, ServerTickPeriodSchedule.averageMillis(4), 0.0001D);
        assertEquals(6.25D, ServerTickPeriodSchedule.averageMillis(8), 0.0001D);
        assertEquals(3.125D, ServerTickPeriodSchedule.averageMillis(16), 0.0001D);
        assertEquals(1.5625D, ServerTickPeriodSchedule.averageMillis(32), 0.0001D);
    }

    @Test
    public void sixtyFourAndOneTwentyEightUseFullTickBurstsAtThirtyTwoCadence() {
        assertArrayEquals(ServerTickPeriodSchedule.cycleMillis(32), ServerTickPeriodSchedule.cycleMillis(64));
        assertArrayEquals(ServerTickPeriodSchedule.cycleMillis(32), ServerTickPeriodSchedule.cycleMillis(128));
        assertEquals(1, ServerTickPeriodSchedule.fullTicksPerOuterTick(32));
        assertEquals(2, ServerTickPeriodSchedule.fullTicksPerOuterTick(64));
        assertEquals(4, ServerTickPeriodSchedule.fullTicksPerOuterTick(128));
        assertEquals(50.0D / 64.0D, ServerTickPeriodSchedule.effectiveAverageMillisPerFullTick(64), 0.0001D);
        assertEquals(50.0D / 128.0D, ServerTickPeriodSchedule.effectiveAverageMillisPerFullTick(128), 0.0001D);
    }

    @Test
    public void invalidMultiplierFallsBackToStockPeriodAndSingleTick() {
        assertArrayEquals(new long[] { 50L }, ServerTickPeriodSchedule.cycleMillis(3));
        assertEquals(50.0D, ServerTickPeriodSchedule.averageMillis(0), 0.0001D);
        assertEquals(1, ServerTickPeriodSchedule.fullTicksPerOuterTick(3));
    }
}
