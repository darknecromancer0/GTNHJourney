package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSnapshotStagerTest {

    @TempDir
    Path temp;

    @Test
    void stageCopiesWorldTreeOutsideLiveWorldAndCleanupRemovesIt() throws Exception {
        Path world = temp.resolve("Cirno world");
        Files.createDirectories(world.resolve("region"));
        Files.write(world.resolve("level.dat"), "level".getBytes(StandardCharsets.UTF_8));
        Files.write(world.resolve("region/r.0.0.mca"), "region".getBytes(StandardCharsets.UTF_8));
        Path backupRoot = temp.resolve("gtnhjourney-backups/Cirno world");

        WorldSnapshotStager.StagedSnapshot staged = WorldSnapshotStager.stage(world.toFile(), backupRoot.toFile());

        File snapshotWorld = staged.worldDirectory();
        assertTrue(snapshotWorld.isDirectory());
        assertEquals("Cirno world", snapshotWorld.getName());
        assertFalse(snapshotWorld.getCanonicalPath().startsWith(world.toFile().getCanonicalPath() + File.separator));
        assertEquals("level", new String(Files.readAllBytes(snapshotWorld.toPath().resolve("level.dat")), StandardCharsets.UTF_8));
        assertEquals(
            "region",
            new String(Files.readAllBytes(snapshotWorld.toPath().resolve("region/r.0.0.mca")), StandardCharsets.UTF_8));

        staged.cleanup();
        assertFalse(snapshotWorld.getParentFile().exists());
    }
}
