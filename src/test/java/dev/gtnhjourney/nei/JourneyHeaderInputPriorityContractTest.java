package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyHeaderInputPriorityContractTest {

    @Test
    void journeyHeaderGetsInputBeforeNeiItemPanel() throws IOException {
        String source = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/NEIGTNHJourneyConfig.java"));

        assertTrue(source.contains("GuiContainerManager.inputHandlers.addFirst(toggle);"),
            "header hitboxes overlap NEI item cells, so the Journey header must get first chance to own the click");
        assertFalse(source.contains("GuiContainerManager.addInputHandler(toggle);"),
            "appending the header handler lets LayoutManager consume an overlapping item click before Journey can cancel it");
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
