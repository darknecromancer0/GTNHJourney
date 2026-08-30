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
    void backupPreparationNeverDisablesVanillaWorldSaving() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");

        assertFalse(source.contains("world.levelSaving = true"), "Journey backup must never disable normal world saving");
        assertFalse(source.contains("WorldSaveState"), "backup must not hold or restore a temporary save-disable state");
        assertTrue(source.contains("WorldSnapshotStager"), "backup should archive a stable staged snapshot instead of live files");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
