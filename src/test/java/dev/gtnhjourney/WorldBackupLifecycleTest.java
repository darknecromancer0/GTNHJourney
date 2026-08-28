package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gtnhjourney.backup.WorldBackupResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WorldBackupLifecycleTest {

    @AfterEach
    void resetGlobalBackupState() {
        GTNHJourney.WORLD_BACKUPS.resetSession();
    }

    @Test
    void serverStartedReanchorsBackupSessionAfterWorldActuallyLoads() {
        GTNHJourney.WORLD_BACKUPS.resetSession();
        GTNHJourney.WORLD_BACKUPS.tryBackup(null, true);
        assertTrue(GTNHJourney.WORLD_BACKUPS.lastDurationMillis() >= 0L);

        new GTNHJourney().serverStarted(null);

        assertEquals(-1L, GTNHJourney.WORLD_BACKUPS.lastDurationMillis());
        assertEquals("No world backup completed this session.", GTNHJourney.WORLD_BACKUPS.lastResult().getMessage());
    }

    @Test
    void serverStoppingRejectsNewBackupWorkBeforeFinalStopCleanup() {
        new GTNHJourney().serverStopping(null);

        WorldBackupResult result = GTNHJourney.WORLD_BACKUPS.tryBackup(null, true);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("stopping"));
    }
}
