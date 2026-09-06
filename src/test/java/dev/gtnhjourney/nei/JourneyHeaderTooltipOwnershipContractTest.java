package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyHeaderTooltipOwnershipContractTest {

    @Test
    void hoveringJourneyControlsClearsUnderlyingGuiTooltipBeforeAddingButtonTip() throws IOException {
        String source = compact(read("src/main/java/dev/gtnhjourney/nei/JourneyNEIToggleWidget.java"));
        assertTrue(source.contains("if(ownsTooltip(mousex,mousey))currenttip.clear();"),
            "Journey controls must exclusively own tooltip hover so an item tooltip underneath cannot survive");
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
