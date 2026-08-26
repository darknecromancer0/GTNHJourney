package dev.gtnhjourney.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class JourneyRuntimeCountersCreativeSafetyTest {

    @AfterEach
    public void reset() {
        JourneyRuntimeCounters.reset();
    }

    @Test
    public void creativeSafetyCountsOnlyPositiveRemovedVariants() {
        JourneyRuntimeCounters.reset();
        JourneyRuntimeCounters.creativeUnsafeFlaskVariantsRemoved(3);
        JourneyRuntimeCounters.creativeUnsafeFlaskVariantsRemoved(-5);

        assertEquals(3L, JourneyRuntimeCounters.snapshot().getCreativeUnsafeFlaskVariantsRemoved());
    }
}
