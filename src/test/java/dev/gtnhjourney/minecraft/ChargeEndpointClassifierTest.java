package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ChargeEndpointClassifierTest {

    @Test
    public void emptyAndPartialExtractableChargeCollapseToBaseWhileFullStaysDistinct() {
        assertEquals(
            ChargeEndpointClassifier.State.BASE,
            ChargeEndpointClassifier.classify(0, 1_000_000, false));
        assertEquals(
            ChargeEndpointClassifier.State.BASE,
            ChargeEndpointClassifier.classify(250_000, 1_000_000, true));
        assertEquals(
            ChargeEndpointClassifier.State.FULL,
            ChargeEndpointClassifier.classify(1_000_000, 1_000_000, true));
    }

    @Test
    public void positiveNonExtractableOrInvalidChargeFailsClosed() {
        assertEquals(
            ChargeEndpointClassifier.State.EXACT,
            ChargeEndpointClassifier.classify(250_000, 1_000_000, false));
        assertEquals(
            ChargeEndpointClassifier.State.EXACT,
            ChargeEndpointClassifier.classify(-1, 1_000_000, false));
        assertEquals(
            ChargeEndpointClassifier.State.EXACT,
            ChargeEndpointClassifier.classify(0, 0, false));
    }
}
