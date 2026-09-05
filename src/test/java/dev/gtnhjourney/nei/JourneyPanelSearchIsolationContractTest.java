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
    public void directJourneyPanelUsesNativeNeiFiltersWithoutReintroducingHiddenSearch() throws IOException {
        Path source = Paths.get("src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue(text.contains("synchronizeSearchWidgetVisibility();"));
        assertTrue(text.contains("Minecraft minecraft = Minecraft.getMinecraft();"));
        assertTrue(text.contains("minecraft.currentScreen"));
        assertTrue(text.contains("LayoutManager.layout((GuiContainer) currentScreen);"));
        assertTrue(text.contains("JourneyNeiFilterPipeline.snapshotActiveFilters()"));
        assertTrue(text.indexOf("synchronizeSearchWidgetVisibility();")
            < text.indexOf("JourneyNeiFilterPipeline.snapshotActiveFilters()"));
        assertFalse(text.contains("LayoutManager.searchField.getFilter()"));
        assertFalse(text.contains("ItemList.getItemListFilter()"));
    }
}
