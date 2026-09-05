package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyHeaderDepthLayerContractTest {

    @Test
    void postPassUsesNeiTwoDimensionalContextSoItemDepthCannotCoverHeader() throws IOException {
        String source = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/JourneyNEIToggleWidget.java"));
        int postStart = source.indexOf("voidpostRenderObjects(GuiContainergui,intmousex,intmousey)");
        int slotStart = source.indexOf("voidrenderSlotUnderlay", postStart);
        assertTrue(postStart >= 0 && slotStart > postStart);

        String postPass = source.substring(postStart, slotStart);
        assertTrue(postPass.contains("NEIClientUtils.gl2DRenderContext("),
            "NEI leaves depth testing enabled after item rendering; Journey must paint the header in NEI's 2D context");
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
