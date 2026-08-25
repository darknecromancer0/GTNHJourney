package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    public void cadenceIterationSkipsNullAndIsolatesOneBrokenPlayer() {
        final AtomicInteger calls = new AtomicInteger();
        JourneySnapshotTicker.CadenceAction<String> action = new JourneySnapshotTicker.CadenceAction<String>() {

            @Override
            public void apply(String value) {
                if ("broken".equals(value)) throw new IllegalStateException("broken optional state");
                calls.incrementAndGet();
            }
        };

        assertEquals(0, JourneySnapshotTicker.forEachAtCadence(2399L, Arrays.asList("a", "b"), action));
        assertEquals(2, JourneySnapshotTicker.forEachAtCadence(2400L, Arrays.asList("a", null, "broken", "b"), action));
        assertEquals(2, calls.get());
    }
}
