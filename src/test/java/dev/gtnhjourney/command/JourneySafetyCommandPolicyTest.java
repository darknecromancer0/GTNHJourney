package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JourneySafetyCommandPolicyTest {

    @Test
    void backupActionsAreStrictAndMutationsRequireAdmin() {
        assertTrue(JourneySafetyCommandPolicy.isBackupAction("status"));
        assertTrue(JourneySafetyCommandPolicy.isBackupAction("now"));
        assertTrue(JourneySafetyCommandPolicy.isBackupAction("on"));
        assertTrue(JourneySafetyCommandPolicy.isBackupAction("off"));
        assertFalse(JourneySafetyCommandPolicy.isBackupAction("later"));

        assertFalse(JourneySafetyCommandPolicy.requiresAdmin("backup", "status"));
        assertTrue(JourneySafetyCommandPolicy.requiresAdmin("backup", "now"));
        assertTrue(JourneySafetyCommandPolicy.requiresAdmin("backup", "on"));
        assertTrue(JourneySafetyCommandPolicy.requiresAdmin("backup", "off"));
    }

    @Test
    void explosionStatusIsReadableButChangesRequireAdmin() {
        assertTrue(JourneySafetyCommandPolicy.isExplosionAction("status"));
        assertTrue(JourneySafetyCommandPolicy.isExplosionAction("on"));
        assertTrue(JourneySafetyCommandPolicy.isExplosionAction("off"));
        assertFalse(JourneySafetyCommandPolicy.isExplosionAction("toggle"));

        assertFalse(JourneySafetyCommandPolicy.requiresAdmin("explosions", "status"));
        assertTrue(JourneySafetyCommandPolicy.requiresAdmin("explosions", "on"));
        assertTrue(JourneySafetyCommandPolicy.requiresAdmin("explosions", "off"));
    }

    @Test
    void cleanseTargetsCallerWithoutAdminMutationPermission() {
        assertFalse(JourneySafetyCommandPolicy.requiresAdmin("cleanse", ""));
    }
}
