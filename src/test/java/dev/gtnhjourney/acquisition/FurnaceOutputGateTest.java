package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FurnaceOutputGateTest {

    @Test
    public void onlyNewNonEmptyIdentityTransitionsFire() {
        FurnaceOutputGate gate = new FurnaceOutputGate();
        gate.prime(0, false);

        assertTrue(gate.observe(12345, true));
        assertFalse(gate.observe(12345, true));
        assertFalse(gate.observe(0, false));
        assertTrue(gate.observe(67890, true));
    }

    @Test
    public void primedExistingOutputDoesNotFabricateCompletion() {
        FurnaceOutputGate gate = new FurnaceOutputGate();
        gate.prime(12345, true);

        assertFalse(gate.observe(12345, true));
        assertFalse(gate.observe(0, false));
        assertTrue(gate.observe(12345, true));
    }
}
