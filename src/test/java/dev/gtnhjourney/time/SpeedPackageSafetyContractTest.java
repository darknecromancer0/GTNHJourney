package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class SpeedPackageSafetyContractTest {

    @Test
    public void speedImplementationNeverDirectlyTicksTileEntities() throws IOException {
        Path root = Paths.get("src/main/java/dev/gtnhjourney/time");
        assertTrue(Files.isDirectory(root));
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    assertFalse(source.contains("TileEntity.updateEntity"), path.toString());
                    assertFalse(source.contains("updateEntity()"), path.toString());
                } catch (IOException failure) {
                    throw new AssertionError(failure);
                }
            });
        }
    }

    @Test
    public void backupCadenceDoesNotDependOnSpeedSubsystem() throws IOException {
        String policy = read("src/main/java/dev/gtnhjourney/backup/WorldBackupPolicy.java");
        String coordinator = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");
        assertFalse(policy.contains("JourneySpeed"));
        assertFalse(coordinator.contains("JourneySpeed"));
        assertTrue(coordinator.contains("clock.nowMillis()"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
