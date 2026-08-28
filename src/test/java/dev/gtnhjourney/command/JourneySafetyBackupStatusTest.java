package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gtnhjourney.backup.WorldBackupResult;
import org.junit.jupiter.api.Test;

class JourneySafetyBackupStatusTest {

    @Test
    void reportsIdleDurationAndLastResult() {
        String text = JourneySafetyCommandHandler.backupStatusText(
            true,
            300,
            3,
            false,
            5_432L,
            WorldBackupResult.success(null));

        assertTrue(text.contains("enabled"));
        assertTrue(text.contains("300s"));
        assertTrue(text.contains("keep 3"));
        assertTrue(text.contains("idle"));
        assertTrue(text.contains("5.4s"));
        assertTrue(text.contains("Backup completed."));
    }

    @Test
    void reportsRunningAndNoCompletedDurationYet() {
        String text = JourneySafetyCommandHandler.backupStatusText(
            false,
            300,
            3,
            true,
            -1L,
            WorldBackupResult.failure("No world backup completed this session."));

        assertTrue(text.contains("disabled"));
        assertTrue(text.contains("running"));
        assertTrue(text.contains("last duration n/a"));
        assertTrue(text.contains("No world backup completed this session."));
    }
}
