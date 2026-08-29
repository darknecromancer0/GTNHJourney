package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class RenderBoundaryRecoveryTest {

    @Test
    public void cleanBoundaryDoesNotFinishAnything() {
        AtomicInteger finishes = new AtomicInteger();

        boolean recovered = RenderBoundaryRecovery.finishDanglingBatch(false, finishes::incrementAndGet);

        assertFalse(recovered);
        assertEquals(0, finishes.get());
    }

    @Test
    public void danglingBatchIsFinishedExactlyOnce() {
        AtomicInteger finishes = new AtomicInteger();

        boolean recovered = RenderBoundaryRecovery.finishDanglingBatch(true, finishes::incrementAndGet);

        assertTrue(recovered);
        assertEquals(1, finishes.get());
    }
}
