package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneySpeedStateTest {

    @Test
    public void defaultsAndResetsToOne() {
        JourneySpeedState state = new JourneySpeedState();
        assertEquals(1, state.multiplier());
        assertEquals(20, state.targetTps());

        assertTrue(state.trySetMultiplier(8));
        assertEquals(8, state.multiplier());
        assertEquals(160, state.targetTps());

        state.reset();
        assertEquals(1, state.multiplier());
        assertEquals(20, state.targetTps());
    }

    @Test
    public void acceptedSetIsExactlyOneTwoFourEight() {
        JourneySpeedState state = new JourneySpeedState();
        int[] accepted = { 1, 2, 4, 8 };
        for (int multiplier : accepted) {
            assertTrue(state.trySetMultiplier(multiplier));
            assertEquals(multiplier, state.multiplier());
        }

        int[] rejected = { Integer.MIN_VALUE, -8, -1, 0, 3, 5, 6, 7, 9, 16, Integer.MAX_VALUE };
        for (int multiplier : rejected) {
            int before = state.multiplier();
            assertFalse(state.trySetMultiplier(multiplier));
            assertEquals(before, state.multiplier());
        }
    }
}
