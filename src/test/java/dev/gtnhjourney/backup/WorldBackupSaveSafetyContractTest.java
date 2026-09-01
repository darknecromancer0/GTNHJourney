package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class WorldBackupSaveSafetyContractTest {

    @Test
    void backgroundStagingSuspendsSavingAfterFlushButOnlyServerThreadMayRestoreIt() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");

        int flush = source.indexOf("ThreadedFileIOBase.threadedIOInstance.waitForFinish()");
        int suspend = source.indexOf("world.levelSaving = true");
        int stage = source.indexOf("WorldSnapshotStager.stage");
        int archiveMethod = source.indexOf("public WorldBackupResult archive()", stage);
        int cleanupMethod = source.indexOf("public void cleanup()", archiveMethod);
        int poll = source.indexOf("public synchronized WorldBackupResult pollCompletion()");
        int workerFinishedGate = source.indexOf("if (!running || !workerFinished) return null;", poll);
        int resumeFromPoll = source.indexOf("resumeLiveSavingIfReady()", poll);

        assertTrue(flush >= 0, "backup must wait for queued chunk IO before taking the snapshot");
        assertTrue(suspend > flush, "world saving must be suspended only after the flushed disk state is complete");
        assertTrue(stage > suspend, "save suspension must cover the isolated background staging copy");
        assertTrue(archiveMethod >= 0 && cleanupMethod > archiveMethod, "prepared backup archive/cleanup methods must exist");

        String workerArchive = source.substring(archiveMethod, cleanupMethod);
        assertFalse(
            workerArchive.contains("restoreSaveState();"),
            "GTNHJourney-WorldBackup must never mutate WorldServer.levelSaving from the backup worker");

        assertTrue(
            resumeFromPoll > poll && resumeFromPoll < workerFinishedGate,
            "server END ticks must resume live world saving as soon as staging finishes, even while ZIP creation continues");
        assertTrue(
            source.contains("private volatile boolean snapshotStageFinished"),
            "worker-to-server staging handoff must have explicit cross-thread visibility");
        assertTrue(source.contains("previousLevelSaving"), "backup must preserve every world's prior save setting");
        assertTrue(
            source.contains("world.levelSaving = previousLevelSaving[i]"),
            "server-thread cleanup must restore every world's prior save setting");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
