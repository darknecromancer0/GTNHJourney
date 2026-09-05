package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JourneyHeaderLayoutTest {

    @Test
    void serviceButtonsSitImmediatelyLeftOfNativeGWithoutOverlap() {
        JourneyHeaderLayout.Layout layout = JourneyHeaderLayout.layout(10, 4, 16, 300, 16);

        assertTrue(layout.scanVisible);
        assertTrue(layout.debugVisible);
        assertFalse(layout.scan.overlaps(layout.debug));
        assertFalse(layout.debug.overlaps(layout.nativeG));
        assertFalse(layout.scan.overlaps(layout.nativeG));
        assertTrue(layout.scan.x > layout.order.x);
        assertTrue(layout.debug.x > layout.scan.x);
        assertTrue(layout.nativeG.x > layout.debug.x);
    }

    @Test
    void narrowHeaderHidesServiceButtonsBeforeOverlappingCoreSortControls() {
        JourneyHeaderLayout.Layout layout = JourneyHeaderLayout.layout(10, 4, 16, 205, 16);

        assertFalse(layout.debugVisible);
        if (layout.scanVisible) assertFalse(layout.scan.overlaps(layout.order));
        assertFalse(layout.order.overlaps(layout.nativeG));
    }

    @Test
    void neiButtonIsWiderThanSingleLetterJourneyButtons() {
        JourneyHeaderLayout.Layout layout = JourneyHeaderLayout.layout(10, 4, 16, 300, 16);
        assertTrue(layout.nei.w > layout.researched.w);
    }
}
