package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release117ContractTest {

    @Test
    public void release117BackupRegressionRemainsDocumentedAndSafe() throws IOException {
        Path liveTest = Paths.get("docs/v1.1.7-live-test.md");
        assertTrue(Files.isRegularFile(liveTest), "missing v1.1.7 backup live-test checklist");
        String document = read(liveTest.toString()).toLowerCase();
        assertTrue(document.contains("10-20 second"));
        assertTrue(document.contains("staging"));
        assertTrue(document.contains("background"));
        assertTrue(document.contains("vanilla world saving remains enabled throughout"));

        String coordinator = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");
        String stager = read("src/main/java/dev/gtnhjourney/backup/WorldSnapshotStager.java");
        int prepare = coordinator.indexOf("public PreparedBackup prepare(MinecraftServer server)");
        int preparedClass = coordinator.indexOf("private static final class MinecraftPreparedBackup");
        int stage = coordinator.indexOf("WorldSnapshotStager.stage");
        int restore = coordinator.indexOf("restoreSaveState();", stage);
        int archive = coordinator.indexOf("writer.write", stage);
        assertTrue(prepare >= 0 && preparedClass > prepare && stage > preparedClass);
        assertTrue(restore > stage && archive > restore,
            "any consistency save freeze must end after background staging and before the longer ZIP archive");
        assertTrue(stager.contains("STABLE_COPY_ATTEMPTS = 3"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
