package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneyRetrieveClickPolicyTest {

    @Test
    public void leftClickRequestsOneItem() {
        assertEquals(1, JourneyRetrieveClickPolicy.requestedAmount(0, false, 64));
        assertEquals(1, JourneyRetrieveClickPolicy.requestedAmount(0, true, 64));
    }

    @Test
    public void rightClickRequestsOneNaturalStack() {
        assertEquals(64, JourneyRetrieveClickPolicy.requestedAmount(1, false, 64));
        assertEquals(1, JourneyRetrieveClickPolicy.requestedAmount(1, false, 1));
    }

    @Test
    public void fillInventoryGestureIsShiftRightClickOnly() {
        assertTrue(JourneyRetrieveClickPolicy.shouldFillInventory(1, true));
        assertFalse(JourneyRetrieveClickPolicy.shouldFillInventory(1, false));
        assertFalse(JourneyRetrieveClickPolicy.shouldFillInventory(0, true));
        assertFalse(JourneyRetrieveClickPolicy.shouldFillInventory(0, false));
    }
}
