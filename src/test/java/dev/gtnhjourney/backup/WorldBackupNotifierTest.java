package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

public class WorldBackupNotifierTest {

    @Test
    public void successMessageIncludesCompletionAndDuration() {
        String message = WorldBackupNotifier.completionMessage(WorldBackupResult.success(new File("backup-test.zip")), 125000L);

        assertTrue(message.contains("Backup completed"));
        assertTrue(message.contains("backup-test.zip"));
        assertTrue(message.contains("2m 5s"));
    }

    @Test
    public void failureMessageIncludesFailureAndDuration() {
        String message = WorldBackupNotifier.completionMessage(WorldBackupResult.failure("disk full"), 9000L);

        assertTrue(message.contains("Backup failed"));
        assertTrue(message.contains("disk full"));
        assertTrue(message.contains("9s"));
    }
}
