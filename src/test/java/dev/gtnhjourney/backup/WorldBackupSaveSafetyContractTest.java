package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class WorldBackupSaveSafetyContractTest {

    @Test
    void backgroundStagingTemporarilySuspendsWorldSavingAfterFlushAndRestoresBeforeZip() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");

        int flush = source.indexOf("ThreadedFileIOBase.threadedIOInstance.waitForFinish()");
        int suspend = source.indexOf("world.levelSaving = true");
        int stage = source.indexOf("WorldSnapshotStager.stage");
        int restore = source.indexOf("restoreSaveState();", stage);
        int archive = source.indexOf("writer.write", stage);
        assertTrue(flush >= 0, "backup must wait for queued chunk IO before taking the snapshot");
        assertTrue(suspend > flush, "world saving must be suspended only after the flushed disk state is complete");
        assertTrue(stage > suspend, "save suspension must cover the isolated background staging copy");
        assertTrue(restore > stage && archive > restore,
            "world saving must be restored as soon as staging ends, before the longer ZIP archive");
        assertTrue(source.contains("previousLevelSaving"), "backup must preserve every world's prior save setting");
        assertTrue(
            source.contains("world.levelSaving = previousLevelSaving[i]"),
            "backup cleanup must restore every world's prior save setting");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
