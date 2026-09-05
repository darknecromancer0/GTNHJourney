package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneySortDropdownContractTest {

    @Test
    void openDropdownOptionsRenderInPostPassAboveNeiItems() throws IOException {
        String dropdown = read("src/main/java/dev/gtnhjourney/nei/JourneySortDropdown.java");
        String widget = read("src/main/java/dev/gtnhjourney/nei/JourneyNEIToggleWidget.java");

        assertTrue(dropdown.contains("void drawMain("),
            "dropdown must separate its compact header button from the popup overlay pass");
        assertTrue(dropdown.contains("void drawOverlay("),
            "dropdown must expose an overlay-only draw pass for open options");
        assertTrue(widget.contains("postRenderObjects(GuiContainer gui, int mousex, int mousey)"));
        assertTrue(widget.contains("groupDropdown.drawOverlay(mousex, mousey);"));
        assertTrue(widget.contains("orderDropdown.drawOverlay(mousex, mousey);"));
        assertFalse(widget.contains("groupDropdown.draw(mousex, mousey);"),
            "open popup options must no longer render in the ordinary object pass underneath NEI items");
    }

    @Test
    void handledJourneyHeaderClicksCancelNativeItemMouseDown() throws IOException {
        String widget = read("src/main/java/dev/gtnhjourney/nei/JourneyNEIToggleWidget.java");

        assertTrue(widget.contains("cancelNativeItemClick()"),
            "Journey-owned clicks need an explicit NEI item-panel mouseDown cancellation before mouse-up");
        assertTrue(widget.contains("ItemPanels.itemPanel.mouseDownSlot = -1;"),
            "cancelling the native mouseDown slot prevents popup selection from issuing the item underneath on mouse-up");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
