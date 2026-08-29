package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class WorldBackupCompletionNotificationContractTest {

    @Test
    public void tickerForwardsCompletedWorkerResultInsteadOfDiscardingIt() throws IOException {
        String ticker = read("src/main/java/dev/gtnhjourney/backup/WorldBackupTicker.java");

        assertTrue(ticker.contains("WorldBackupResult completed = coordinator.pollCompletion()"));
        assertTrue(ticker.contains("notifier.notifyCompletion(server, completed, coordinator.lastDurationMillis())"));
    }

    @Test
    public void completionNotifierHasSuccessFailureAndDurationText() throws IOException {
        String notifier = read("src/main/java/dev/gtnhjourney/backup/WorldBackupNotifier.java");

        assertTrue(notifier.contains("Backup completed"));
        assertTrue(notifier.contains("Backup failed"));
        assertTrue(notifier.contains("formatDuration"));
    }

    private static String read(String path) throws IOException {
        Path file = Paths.get(path);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
