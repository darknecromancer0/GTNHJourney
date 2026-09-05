package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyHeaderRenderLayerContractTest {

    @Test
    void entireJourneyHeaderPaintsInPostPassAboveNeiItemCells() throws IOException {
        String source = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/JourneyNEIToggleWidget.java"));
        int renderStart = source.indexOf("voidrenderObjects(GuiContainergui,intmousex,intmousey)");
        int postStart = source.indexOf("voidpostRenderObjects(GuiContainergui,intmousex,intmousey)");
        int slotStart = source.indexOf("voidrenderSlotUnderlay", postStart);

        assertTrue(renderStart >= 0 && postStart > renderStart && slotStart > postStart);
        String ordinaryPass = source.substring(renderStart, postStart);
        String postPass = source.substring(postStart, slotStart);

        assertFalse(ordinaryPass.contains(".draw(mousex,mousey)"),
            "Journey header controls must not paint in the ordinary draw-handler pass where NEI item cells can cover them");
        assertFalse(ordinaryPass.contains("drawMain(mousex,mousey)"),
            "Journey dropdown roots must not paint underneath later NEI draw handlers");

        assertTrue(postPass.contains("neiButton.draw(mousex,mousey)"));
        assertTrue(postPass.contains("researchButton.draw(mousex,mousey)"));
        assertTrue(postPass.contains("favouriteButton.draw(mousex,mousey)"));
        assertTrue(postPass.contains("creativeButton.draw(mousex,mousey)"));
        assertTrue(postPass.contains("deleteButton.draw(mousex,mousey)"));
        assertTrue(postPass.contains("latestButton.draw(mousex,mousey)"));
        assertTrue(postPass.contains("groupDropdown.drawMain(mousex,mousey)"));
        assertTrue(postPass.contains("orderDropdown.drawMain(mousex,mousey)"));
        assertTrue(postPass.contains("groupDropdown.drawOverlay(mousex,mousey)"));
        assertTrue(postPass.contains("orderDropdown.drawOverlay(mousex,mousey)"));

        assertTrue(postPass.indexOf("groupDropdown.drawOverlay(mousex,mousey)")
            > postPass.indexOf("groupDropdown.drawMain(mousex,mousey)"));
        assertTrue(postPass.indexOf("orderDropdown.drawOverlay(mousex,mousey)")
            > postPass.indexOf("orderDropdown.drawMain(mousex,mousey)"));
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
