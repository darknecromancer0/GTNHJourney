package dev.gtnhjourney.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JourneyConfigScanIntervalTest {

    @Test
    public void defaultAndLegacyIntervalsUseFiveTickFloor() {
        assertEquals(5, JourneyConfig.DEFAULT_INVENTORY_SCAN_INTERVAL_TICKS);
        assertEquals(5, JourneyConfig.normalizeInventoryScanIntervalTicks(1));
        assertEquals(5, JourneyConfig.normalizeInventoryScanIntervalTicks(2));
        assertEquals(5, JourneyConfig.normalizeInventoryScanIntervalTicks(5));
        assertEquals(20, JourneyConfig.normalizeInventoryScanIntervalTicks(20));
        assertEquals(40, JourneyConfig.normalizeInventoryScanIntervalTicks(40));
    }
}
