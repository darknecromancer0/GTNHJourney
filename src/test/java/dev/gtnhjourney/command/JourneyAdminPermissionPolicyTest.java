package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JourneyAdminPermissionPolicyTest {

    @Test
    void ownerOrOperatorMayMutateSafetySettings() {
        assertTrue(JourneyAdminPermissionPolicy.mayMutate(true, true, false));
        assertTrue(JourneyAdminPermissionPolicy.mayMutate(false, false, true));
        assertFalse(JourneyAdminPermissionPolicy.mayMutate(true, false, false));
        assertFalse(JourneyAdminPermissionPolicy.mayMutate(false, false, false));
    }
}
