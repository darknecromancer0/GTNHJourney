package dev.gtnhjourney.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JourneySafetyConfigPolicyTest {

    @Test
    void backupDefaultsAndBoundsAreStable() {
        assertEquals(300, JourneyConfig.normalizeWorldBackupIntervalSeconds(300));
        assertEquals(60, JourneyConfig.normalizeWorldBackupIntervalSeconds(0));
        assertEquals(86400, JourneyConfig.normalizeWorldBackupIntervalSeconds(Integer.MAX_VALUE));
        assertEquals(3, JourneyConfig.normalizeWorldBackupRetention(3));
        assertEquals(1, JourneyConfig.normalizeWorldBackupRetention(0));
        assertEquals(32, JourneyConfig.normalizeWorldBackupRetention(Integer.MAX_VALUE));
    }
}
