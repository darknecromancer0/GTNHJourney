package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.nei.guihook.IContainerTooltipHandler;

class JourneyTooltipManagerPriorityTest {

    @Test
    void movesJourneyLastInsideAnExistingManagerTooltipList() {
        JourneyNEIToggleWidget journey = new JourneyNEIToggleWidget();
        JourneyNEIToggleWidget laterHandler = new JourneyNEIToggleWidget();
        List<IContainerTooltipHandler> handlers = new LinkedList<IContainerTooltipHandler>();
        handlers.add(journey);
        handlers.add(laterHandler);

        JourneyNeiHandlerPriority.ensureTooltipLast(handlers, journey);

        assertSame(journey, handlers.get(handlers.size() - 1));
    }

    @Test
    void runtimePriorityAlsoRepairsCurrentManagersDetachedTooltipCopy() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/nei/JourneyNeiHandlerPriority.java");
        assertTrue(source.contains("instanceTooltipHandlers"),
            "Reordering only GuiContainerManager.tooltipHandlers cannot repair managers that copied the list at construction");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
