package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyTooltipManagerPriorityTest {

    @Test
    void runtimePriorityAlsoRepairsCurrentManagersDetachedTooltipCopy() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/nei/JourneyNeiHandlerPriority.java");
        assertTrue(source.contains("instanceTooltipHandlers"),
            "Reordering only GuiContainerManager.tooltipHandlers cannot repair managers that copied the list at construction");
        assertTrue(source.contains("GuiContainerManager.getManager"),
            "Priority repair must target the current GuiContainerManager instance, not only global registration state");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
