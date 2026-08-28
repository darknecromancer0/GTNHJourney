package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldBackupPolicyTest {

    @Test
    void exactFiveMinuteDefaultCadence() {
        assertFalse(WorldBackupPolicy.isDue(299_999L, 0L, 300, true));
        assertTrue(WorldBackupPolicy.isDue(300_000L, 0L, 300, true));
        assertFalse(WorldBackupPolicy.isDue(999_999L, 0L, 300, false));
    }

    @Test
    void cadenceIsMeasuredFromLastSuccessfulBackup() {
        assertFalse(WorldBackupPolicy.isDue(599_999L, 300_000L, 300, true));
        assertTrue(WorldBackupPolicy.isDue(600_000L, 300_000L, 300, true));
    }
}
