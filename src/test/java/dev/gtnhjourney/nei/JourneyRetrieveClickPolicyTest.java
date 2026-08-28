package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(64, JourneyRetrieveClickPolicy.requestedAmount(1, true, 64));
        assertEquals(1, JourneyRetrieveClickPolicy.requestedAmount(1, false, 1));
    }
}
