package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JourneySpeedModeTest {

    @Test
    public void machinesIsTheSafeDefault() {
        JourneySpeedState state = new JourneySpeedState();
        assertEquals(JourneySpeedMode.MACHINES, state.mode());
    }

    @Test
    public void modeNamesAreStableForCommandsAndStatus() {
        assertEquals(JourneySpeedMode.MACHINES, JourneySpeedMode.parse("machines"));
        assertEquals(JourneySpeedMode.WORLD, JourneySpeedMode.parse("world"));
        assertEquals(null, JourneySpeedMode.parse("other"));
    }
}
