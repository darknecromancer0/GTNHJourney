package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneyPanelSearchIsolationContractTest {

    @Test
    public void directJourneyPanelUsesOnlyVisibleNeiSearchInsteadOfAllGlobalItemFilters() throws IOException {
        Path source = Paths.get("src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue(text.contains("LayoutManager.searchField.getFilter()"));
        assertTrue(text.contains("LayoutManager.searchField.isVisible()"));
        assertFalse(text.contains("ItemList.getItemListFilter()"));
    }
}
