package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JourneyCommandHintOverlayTest {

    @Test
    public void shortListsStartAtZero() {
        assertEquals(0, JourneyCommandHintOverlay.visibleWindowStart(7, 6, 10));
    }

    @Test
    public void longListsScrollAroundSelectedEntry() {
        assertEquals(0, JourneyCommandHintOverlay.visibleWindowStart(30, 0, 10));
        assertEquals(7, JourneyCommandHintOverlay.visibleWindowStart(30, 12, 10));
        assertEquals(20, JourneyCommandHintOverlay.visibleWindowStart(30, 29, 10));
    }
}
