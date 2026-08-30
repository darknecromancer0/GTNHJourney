package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneySpeedModesCommandContractTest {

    @Test
    public void commandExposesMachinesWorldAndStatusClearly() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/command/CommandJourney.java");

        assertTrue(source.contains("JourneySpeedMode.MACHINES"));
        assertTrue(source.contains("JourneySpeedMode.parse(args[1])"));
        assertTrue(source.contains("World remains 20 TPS"));
        assertTrue(source.contains("Journey speed set to WORLD"));
        assertTrue(source.contains("/journey speed machines|world"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
