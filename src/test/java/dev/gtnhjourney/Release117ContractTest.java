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
        assertTrue(document.contains("live world saving resumes immediately after staging on the server thread"));

        String coordinator = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");
        String stager = read("src/main/java/dev/gtnhjourney/backup/WorldSnapshotStager.java");
        int prepare = coordinator.indexOf("public PreparedBackup prepare(MinecraftServer server)");
        int preparedClass = coordinator.indexOf("private static final class MinecraftPreparedBackup");
        int archiveMethod = coordinator.indexOf("public WorldBackupResult archive()", preparedClass);
        int stage = coordinator.indexOf("WorldSnapshotStager.stage", archiveMethod);
        int archive = coordinator.indexOf("writer.write", stage);
        int poll = coordinator.indexOf("public synchronized WorldBackupResult pollCompletion()");
        int workerFinishedGate = coordinator.indexOf("if (!running || !workerFinished) return null;", poll);
        int resume = coordinator.indexOf("resumeLiveSavingIfReady()", poll);
        assertTrue(prepare >= 0 && preparedClass > prepare && archiveMethod > preparedClass && stage > archiveMethod);
        assertTrue(archive > stage, "ZIP archive must use the completed isolated staging copy");
        assertTrue(resume > poll && resume < workerFinishedGate,
            "save suspension must end from a server tick as soon as staging completes, before waiting for ZIP completion");
        assertTrue(coordinator.contains("private volatile boolean snapshotStageFinished"));
        assertTrue(stager.contains("STABLE_COPY_ATTEMPTS = 3"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
