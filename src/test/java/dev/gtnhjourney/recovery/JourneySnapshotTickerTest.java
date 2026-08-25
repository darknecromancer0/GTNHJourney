package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneySnapshotTickerTest {

    @Test
    public void cadenceUsesExactPositive2400TickBoundaries() {
        assertFalse(JourneySnapshotTicker.isCadenceTick(0L));
        assertFalse(JourneySnapshotTicker.isCadenceTick(2399L));
        assertTrue(JourneySnapshotTicker.isCadenceTick(2400L));
        assertFalse(JourneySnapshotTicker.isCadenceTick(2401L));
        assertTrue(JourneySnapshotTicker.isCadenceTick(4800L));
    }
}
