package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneyNativeSearchDelegationContractTest {

    @Test
    public void journeyDelegatesSearchGrammarToLiveNeiProviders() throws IOException {
        String pipeline = read("src/main/java/dev/gtnhjourney/nei/JourneyNeiFilterPipeline.java");

        assertTrue(pipeline.contains("ItemList.itemFilterers"));
        assertTrue(pipeline.contains("provider == LayoutManager.searchField"));
        assertTrue(pipeline.contains("provider.getFilter()"));
        assertTrue(pipeline.contains("LayoutManager.searchField.isVisible()"));
        assertFalse(pipeline.contains("Pattern.compile"));
        assertFalse(pipeline.contains("SearchTokenParser"));
        assertFalse(pipeline.contains("\"@\""));
        assertFalse(pipeline.contains("\"#\""));
        assertFalse(pipeline.contains("\"$\""));
        assertFalse(pipeline.contains("\"%\""));
        assertFalse(pipeline.contains("\"&\""));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
