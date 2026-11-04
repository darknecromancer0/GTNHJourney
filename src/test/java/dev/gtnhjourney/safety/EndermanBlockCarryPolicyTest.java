package dev.gtnhjourney.safety;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EndermanBlockCarryPolicyTest {

    @Test
    void offStopsEndermenEvenWhenVanillaGriefingIsEnabled() {
        assertFalse(EndermanBlockCarryPolicy.mayMoveBlocks(true, false));
    }

    @Test
    void onKeepsVanillaMobGriefingRules() {
        assertTrue(EndermanBlockCarryPolicy.mayMoveBlocks(true, true));
        assertFalse(EndermanBlockCarryPolicy.mayMoveBlocks(false, true));
    }

    @Test
    void offCannotOverrideVanillaDisabledGriefing() {
        assertFalse(EndermanBlockCarryPolicy.mayMoveBlocks(false, false));
    }
}
