package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class CommandJourneyRescanContractTest {

    @Test
    public void rescanUsesSameDeepManualRecoveryPathAsSButton() throws IOException {
        Path source = Paths.get("src/main/java/dev/gtnhjourney/command/CommandJourney.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        int start = text.indexOf("if (\"rescan\".equals(action))");
        int end = text.indexOf("if (\"clear\".equals(action))", start);
        assertTrue(start >= 0 && end > start);
        String block = text.substring(start, end);

        assertTrue(block.contains("ManualInventoryResearchService.scan("));
        assertFalse(block.contains("PlayerInventoryScanner.scan("));
    }
}
