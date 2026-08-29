package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneySpeedIntegrationContractTest {

    @Test
    public void modOwnsOneSessionSpeedControllerAndResetsItAtLifecycleBoundaries() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        assertTrue(source.contains("JourneySpeedController SPEED"));
        assertTrue(source.contains("new ReflectiveServerTickRateAdapter()"));
        assertTrue(count(source, "SPEED.reset();") >= 2);
    }

    @Test
    public void journeyCommandExposesStatusAndAdminMutationPath() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/command/CommandJourney.java");
        assertTrue(source.contains("\"speed\""));
        assertTrue(source.contains("JourneyAdminPermissionPolicy.mayMutate(player)"));
        assertTrue(source.contains("GTNHJourney.SPEED.setMultiplier"));
        assertTrue(source.contains("GTNHJourney.SPEED.targetTps()"));
    }

    @Test
    public void helpDocumentsSessionSpeedSurface() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/command/JourneyHelpText.java");
        assertTrue(source.contains("/journey speed"));
        assertTrue(source.contains("1|2|4|8"));
    }

    private static int count(String text, String needle) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
