package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyHeaderRuntimePriorityContractTest {

    @Test
    void headerReassertsDrawLastInputFirstAndTooltipLastAfterNeiFinishesRegisteringHandlers() throws IOException {
        String widget = compact(read("src/main/java/dev/gtnhjourney/nei/JourneyNEIToggleWidget.java"));
        String priority = compact(read("src/main/java/dev/gtnhjourney/nei/JourneyNeiHandlerPriority.java"));

        assertTrue(widget.contains("JourneyNeiHandlerPriority.ensure(this)"));
        assertTrue(priority.contains("GuiContainerManager.drawHandlers.remove(widget)"));
        assertTrue(priority.contains("GuiContainerManager.drawHandlers.addLast(widget)"));
        assertTrue(priority.contains("GuiContainerManager.inputHandlers.remove(widget)"));
        assertTrue(priority.contains("GuiContainerManager.inputHandlers.addFirst(widget)"));
        assertTrue(priority.contains("GuiContainerManager.tooltipHandlers.remove(widget)"));
        assertTrue(priority.contains("GuiContainerManager.tooltipHandlers.addLast(widget)"));
        assertTrue(widget.contains("groupDropdown.containsOpenPopup(mousex,mousey)"));
        assertTrue(widget.contains("orderDropdown.containsOpenPopup(mousex,mousey)"));
        assertTrue(widget.contains("currenttip.clear()"));
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
