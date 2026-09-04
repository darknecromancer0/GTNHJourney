package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class JourneyNeiFilterRevisionTest {

    @AfterEach
    public void cleanUp() {
        JourneyNeiFilterRevision.reset();
    }

    @Test
    public void invalidationAdvancesRevisionMonotonically() {
        JourneyNeiFilterRevision.reset();
        long before = JourneyNeiFilterRevision.revision();
        JourneyNeiFilterRevision.invalidate();
        assertTrue(JourneyNeiFilterRevision.revision() > before);
    }

    @Test
    public void resetClearsSessionRevision() {
        JourneyNeiFilterRevision.invalidate();
        JourneyNeiFilterRevision.invalidate();
        JourneyNeiFilterRevision.reset();
        assertEquals(0L, JourneyNeiFilterRevision.revision());
    }
}
