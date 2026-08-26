package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class DeepInventoryBackpackIntegrationContractTest {

    @Test
    public void manualDeepCollectorIncludesExternalBackpackSaves() throws IOException {
        Path source = Paths.get("src/main/java/dev/gtnhjourney/acquisition/DeepInventoryResearchCollector.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue(text.contains("BackpackExternalInventoryReader.serializedStacks("));
        assertTrue(text.contains("BackpackExternalInventoryReader.externalInstanceId("));
    }
}
