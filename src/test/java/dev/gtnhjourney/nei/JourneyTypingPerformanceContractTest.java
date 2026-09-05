package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** Guards the search hot path against moving NEI's expensive filtering/sorting back onto the client UI thread. */
public class JourneyTypingPerformanceContractTest {

    @Test
    public void journeyInterceptsCompletedNeiFilterPublicationInsteadOfFilterRestart() throws IOException {
        String config = read("src/main/resources/mixins.gtnhjourney.json");
        String oldMixin = read("src/main/java/dev/gtnhjourney/mixin/RestartableTaskJourneyFilterMixin.java");
        String completionMixin = read("src/main/java/dev/gtnhjourney/mixin/ItemPanelJourneyFilterMixin.java");

        assertFalse(config.contains("\"RestartableTaskJourneyFilterMixin\""),
            "keypress/restart is too early: it makes Journey rebuild synchronously before NEI's worker finishes");
        assertTrue(config.contains("\"ItemPanelJourneyFilterMixin\""));
        assertFalse(oldMixin.contains("JourneyNeiFilterRevision.invalidate()"),
            "legacy restart observer must not schedule one client-thread rebuild per keypress");
        assertTrue(completionMixin.contains("@Mixin(value = ItemPanel.class"));
        assertTrue(completionMixin.contains("@Inject(method = \"updateItemList\""));
        assertTrue(completionMixin.contains("JourneyPanelController.captureCompletedNativeFilter"));
        assertTrue(completionMixin.contains("ci.cancel()"),
            "while Journey owns the panel, NEI's native result must be staged instead of flickering over the Journey list");
    }

    @Test
    public void completedFilterPublishesAStagedResultWithoutCallingStructuralRefresh() throws IOException {
        String tracker = read("src/main/java/dev/gtnhjourney/nei/JourneyNEIRefreshTracker.java");
        String controller = read("src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java");

        assertTrue(tracker.contains("JourneyPanelController.publishCompletedFilter()"));
        assertTrue(controller.contains("captureCompletedNativeFilter("));
        assertTrue(controller.contains("buildPanel(false)"),
            "worker hot path must never invoke LayoutManager.layout");
        assertTrue(controller.contains("ItemList.updateFilter.interrupted()"),
            "a new keystroke must be able to invalidate a long Journey worker build before publication");
        assertTrue(controller.contains("publishCompletedFilter()"));
        assertTrue(controller.contains("stagedFilterResult"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
