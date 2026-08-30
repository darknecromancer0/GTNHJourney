package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release119ContractTest {

    @Test
    public void runtimeMetadataAndTwoSpeedModesAgreeOn119() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String mcmod = read("src/main/resources/mcmod.info");
        String build = read("build.gradle.kts");
        String command = read("src/main/java/dev/gtnhjourney/command/CommandJourney.java");
        String machines = read("src/main/java/dev/gtnhjourney/time/MachineTickAccelerator.java");

        assertTrue(source.contains("public static final String VERSION = \"1.1.9\";"));
        assertTrue(mcmod.contains("\"version\": \"1.1.9\""));
        assertTrue(build.contains("version = \"1.1.9\""));
        assertTrue(command.contains("/journey speed machines|world"));
        assertTrue(command.contains("World remains 20 TPS"));
        assertTrue(machines.contains("loadedTileEntityList"));
        assertTrue(machines.contains("EXTRA_WORK_BUDGET_NANOS"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
