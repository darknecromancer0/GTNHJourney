package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release1125ContractTest {

    @Test
    public void commandHintsAreGlobalServerBackedAndKeyboardNavigable() throws IOException {
        String overlay = read("src/main/java/dev/gtnhjourney/client/JourneyCommandHintOverlay.java");
        String state = read("src/main/java/dev/gtnhjourney/client/ClientCommandSuggestionState.java");
        String keys = read("src/main/java/dev/gtnhjourney/client/CommandHintKeyHandler.java");
        String queue = read("src/main/java/dev/gtnhjourney/network/CommandSuggestionRequestQueue.java");
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/GuiChatCommandHintMixin.java");

        assertTrue(overlay.contains("ClientCommandSuggestionState.requestForInput"));
        assertTrue(overlay.contains("visibleWindowStart"));
        assertFalse(overlay.contains("JourneyCommandSuggestions.forChatText"));
        assertTrue(state.contains("requestCommandSuggestions"));
        assertTrue(state.contains("acceptSelected"));
        assertTrue(keys.contains("KEY_UP = 200"));
        assertTrue(keys.contains("KEY_DOWN = 208"));
        assertTrue(keys.contains("KEY_TAB = 15"));
        assertTrue(queue.contains("getCommandManager()"));
        assertTrue(queue.contains("getPossibleCommands(player, commandPrefix)"));
        assertTrue(mixin.contains("ci.cancel()"));
    }

    @Test
    public void itemPanelRefreshRoutesPreviousPageThroughRetentionPolicyAfterNativeReset() throws IOException {
        String panel = compactWhitespace(read("src/main/java/dev/gtnhjourney/nei/JourneyPanelController.java"));
        String capture = "getGrid().getPage()-1";
        String publish = "ItemPanel.updateItemList(built.visible)";
        String restore = "JourneyPageRetentionPolicy.pageAfterRefresh(previousPage,ItemPanels.itemPanel.getGrid().getNumPages(),resetPage)";
        assertTrue(panel.contains(capture));
        assertTrue(panel.contains(publish));
        assertTrue(panel.contains(restore));
        assertTrue(panel.indexOf(capture) < panel.indexOf(publish));
        assertTrue(panel.indexOf(publish) < panel.indexOf(restore));
    }

    @Test
    public void subsetFilteringUsesNativeRepresentativeButSearchKeepsExactJourneyStack() throws IOException {
        String pipeline = read("src/main/java/dev/gtnhjourney/nei/JourneyNeiFilterPipeline.java");
        String index = read("src/main/java/dev/gtnhjourney/nei/JourneyNativeRepresentativeIndex.java");
        assertTrue(pipeline.contains("usesNativeRepresentative()"));
        assertTrue(pipeline.contains("nativeRepresentative"));
        assertTrue(pipeline.contains("SUBSET_WIDGET"));
        assertTrue(index.contains("ItemStack representative(ItemStack display)"));
        assertTrue(index.contains("families.get(family(key, display))"));
    }

    @Test
    public void finalLiveChecklistCoversHighestRiskPaths() throws IOException {
        Path path = Paths.get("docs/v1.1.25-live-test.md");
        assertTrue(Files.isRegularFile(path));
        String text = read(path.toString()).toLowerCase();
        assertTrue(text.contains("global command hints"));
        assertTrue(text.contains("avartia") || text.contains("avaritia"));
        assertTrue(text.contains("page 2+"));
        assertTrue(text.contains("silverfish"));
        assertTrue(text.contains("keepinventory"));
        assertTrue(text.contains("snapshot latest return"));
        assertTrue(text.contains("ebf"));
    }

    private static String compactWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
