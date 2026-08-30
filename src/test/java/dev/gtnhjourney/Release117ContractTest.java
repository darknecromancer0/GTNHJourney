package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release117ContractTest {

    @Test
    public void runtimeMetadataAndBackupThreadingAgreeOn117() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String mcmod = read("src/main/resources/mcmod.info");
        String build = read("build.gradle.kts");
        String coordinator = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");
        String stager = read("src/main/java/dev/gtnhjourney/backup/WorldSnapshotStager.java");

        assertTrue(source.contains("public static final String VERSION = \"1.1.7\";"));
        assertTrue(mcmod.contains("\"version\": \"1.1.7\""));
        assertTrue(build.contains("version = \"1.1.7\""));
        assertFalse(coordinator.contains("world.levelSaving = true"));

        int prepare = coordinator.indexOf("public PreparedBackup prepare(MinecraftServer server)");
        int preparedClass = coordinator.indexOf("private static final class MinecraftPreparedBackup");
        int stage = coordinator.indexOf("WorldSnapshotStager.stage");
        assertTrue(prepare >= 0 && preparedClass > prepare && stage > preparedClass);
        assertTrue(stager.contains("STABLE_COPY_ATTEMPTS = 3"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
