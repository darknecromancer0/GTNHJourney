package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneyNeiFilterSessionResetContractTest {

    @Test
    public void journeyPanelTeardownAlsoClearsNativeFilterRevision() throws IOException {
        String tracker = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/nei/JourneyNEIRefreshTracker.java")),
            StandardCharsets.UTF_8);

        assertTrue(tracker.contains("JourneyNeiFilterRevision.reset();"));
        assertTrue(tracker.indexOf("JourneyPanelController.clear();") >= 0);
    }
}
