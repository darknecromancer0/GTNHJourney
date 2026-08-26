package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DebugToolPermissionPolicyTest {

    @Test
    public void integratedSingleplayerOwnerMayUseDebugToolWithoutGlobalJourneyPermissionEscalation() {
        assertTrue(DebugToolPermissionPolicy.mayUse(true, true, false));
        assertFalse(DebugToolPermissionPolicy.mayUse(true, false, false));
    }

    @Test
    public void dedicatedServerRequiresOperatorPermission() {
        assertTrue(DebugToolPermissionPolicy.mayUse(false, false, true));
        assertFalse(DebugToolPermissionPolicy.mayUse(false, false, false));
    }

    @Test
    public void integratedNonOwnerWithOperatorPermissionMayStillUseIt() {
        assertTrue(DebugToolPermissionPolicy.mayUse(true, false, true));
    }
}
