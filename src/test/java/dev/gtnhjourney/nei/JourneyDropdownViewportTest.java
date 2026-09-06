package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JourneyDropdownViewportTest {

    @Test
    void keepsPopupInsideRightEdge() {
        assertEquals(532, JourneyDropdownViewport.clampPopupX(590, 108, 640));
    }

    @Test
    void preservesAnchorWhenPopupAlreadyFits() {
        assertEquals(420, JourneyDropdownViewport.clampPopupX(420, 108, 640));
    }

    @Test
    void neverMovesPopupPastLeftEdgeOnVeryNarrowScreens() {
        assertEquals(0, JourneyDropdownViewport.clampPopupX(12, 108, 80));
    }
}
