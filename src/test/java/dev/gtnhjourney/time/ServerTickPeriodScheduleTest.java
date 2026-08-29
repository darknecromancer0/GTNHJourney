package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ServerTickPeriodScheduleTest {

    @Test
    public void periodsRepresentExactRequestedAverageRates() {
        assertArrayEquals(new long[] { 50L }, ServerTickPeriodSchedule.cycleMillis(1));
        assertArrayEquals(new long[] { 25L }, ServerTickPeriodSchedule.cycleMillis(2));
        assertArrayEquals(new long[] { 12L, 13L }, ServerTickPeriodSchedule.cycleMillis(4));
        assertArrayEquals(new long[] { 6L, 6L, 6L, 7L }, ServerTickPeriodSchedule.cycleMillis(8));

        assertEquals(50.0D, ServerTickPeriodSchedule.averageMillis(1), 0.0001D);
        assertEquals(25.0D, ServerTickPeriodSchedule.averageMillis(2), 0.0001D);
        assertEquals(12.5D, ServerTickPeriodSchedule.averageMillis(4), 0.0001D);
        assertEquals(6.25D, ServerTickPeriodSchedule.averageMillis(8), 0.0001D);
    }

    @Test
    public void invalidMultiplierFallsBackToStockPeriod() {
        assertArrayEquals(new long[] { 50L }, ServerTickPeriodSchedule.cycleMillis(3));
        assertEquals(50.0D, ServerTickPeriodSchedule.averageMillis(0), 0.0001D);
    }
}
