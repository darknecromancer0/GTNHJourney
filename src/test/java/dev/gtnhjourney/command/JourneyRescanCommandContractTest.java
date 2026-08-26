package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneyRescanCommandContractTest {

    @Test
    public void rescanCommandUsesTheSameManualDeepScannerAsTheSButton() throws IOException {
        Path source = Paths.get("src/main/java/dev/gtnhjourney/command/CommandJourney.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        String compact = text.replaceAll("\\s+", " ");

        assertTrue(compact.contains("ManualInventoryResearchService.scan(player, GTNHJourney.RESEARCH, GTNHJourney.MUTATIONS)"));
    }
}
