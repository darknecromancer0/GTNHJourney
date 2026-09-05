package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyHeaderStartupVisibilityContractTest {

    @Test
    void coreViewStripDoesNotWaitForPageNextInitialization() throws IOException {
        String source = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/JourneyNEIToggleWidget.java"));

        assertTrue(source.contains("visible=ItemPanels.itemPanel.pagePrev!=null;"),
            "NEI/J/F/C/D should become visible as soon as pagePrev exists");
        assertFalse(source.contains("visible=ItemPanels.itemPanel.pagePrev!=null&&ItemPanels.itemPanel.pageNext!=null;"),
            "pageNext is initialized by a later NEI refresh on some screens and must not gate the whole header");
        assertTrue(source.contains("if(ItemPanels.itemPanel.pageNext==null)"),
            "right-anchored sort/service controls should degrade independently while pageNext is unavailable");
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
