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
    void sortingControlsAreRightAnchoredAwayFromNativePageLabel() {
        JourneyHeaderLayout.Layout layout = JourneyHeaderLayout.layout(10, 4, 16, 300, 16);
        int pageLabelCenter = (10 + 300 + 16) / 2;

        assertTrue(layout.delete.right() <= pageLabelCenter - 18,
            "left Journey view controls must leave the native page label clear");
        assertTrue(layout.latest.x >= pageLabelCenter + 18,
            "L/Group/Order must sit to the right of the native page label instead of following D");
        assertTrue(layout.latest.x < layout.group.x);
        assertTrue(layout.group.x < layout.order.x);
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
