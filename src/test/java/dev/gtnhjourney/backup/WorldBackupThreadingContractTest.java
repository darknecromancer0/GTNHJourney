package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class WorldBackupThreadingContractTest {

    @Test
    void worldSnapshotStagingRunsInsideWorkerArchiveNotServerPrepare() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");
        int prepare = source.indexOf("public PreparedBackup prepare(MinecraftServer server)");
        int preparedClass = source.indexOf("private static final class MinecraftPreparedBackup");
        int stage = source.indexOf("WorldSnapshotStager.stage");

        assertTrue(prepare >= 0 && preparedClass > prepare && stage > preparedClass,
            "full world staging must happen in PreparedBackup.archive() on the backup worker, not in prepare() on the server thread");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
