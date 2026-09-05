package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release1123ContractTest {

    @Test
    public void release1123UsesNativeNeiFilteringAndPreservesPageOnResearchRefresh() throws IOException {
        String policy = read("src/main/java/dev/gtnhjourney/nei/JourneyNeiFilterProviderPolicy.java");
        String pipeline = read("src/main/java/dev/gtnhjourney/nei/JourneyNeiFilterPipeline.java");
        String completionBridge = read("src/main/java/dev/gtnhjourney/mixin/ItemPanelJourneyFilterMixin.java");
        String legacyRestartBridge = read("src/main/java/dev/gtnhjourney/mixin/RestartableTaskJourneyFilterMixin.java");
        String decision = read("src/main/java/dev/gtnhjourney/nei/JourneyRefreshDecision.java");

        assertFalse(policy.contains("SUBSET_WIDGET"), "Journey must not special-case Item Subsets out of NEI filtering");
        assertTrue(pipeline.contains("LayoutManager.searchField"));
        assertTrue(pipeline.contains("provider.getFilter()"));
        assertTrue(completionBridge.contains("ItemPanel.class"));
        assertTrue(completionBridge.contains("updateItemList"));
        assertTrue(completionBridge.contains("captureCompletedNativeFilter"));
        assertFalse(legacyRestartBridge.contains("JourneyNeiFilterRevision.invalidate()"),
            "Journey must not run a client-thread rebuild directly from every NEI filter restart");
        assertFalse(legacyRestartBridge.contains("method = \"restart\""));
        assertTrue(decision.contains("if (filterChanged) return Action.PANEL_FILTER_PUBLISH;"));
        assertTrue(decision.contains("return viewChanged || filterChanged;"));
    }

    @Test
    public void release1123SpawnerResolverFailsClosedWithoutPigFallback() throws IOException {
        String resolver = read("src/main/java/dev/gtnhjourney/debug/PlacedBlockResearchResolver.java");

        assertTrue(resolver.contains("TileEntityMobSpawner"));
        assertTrue(resolver.contains("EntityList.createEntityByName"));
        assertTrue(resolver.contains("EntityList.getEntityID"));
        assertTrue(resolver.contains("if (entityMeta <= 0) return null;"));
        assertFalse(resolver.contains("stringToIDMapping"));
        assertFalse(resolver.contains("new ItemStack(item, 1, 90)"));
    }

    @Test
    public void release1123HasCompleteLiveRegressionChecklist() throws IOException {
        Path path = Paths.get("docs/v1.1.23-live-test.md");
        assertTrue(Files.isRegularFile(path), "missing v1.1.23 live-test checklist");
        String document = read(path.toString()).toLowerCase();

        assertTrue(document.contains("silverfish"));
        assertTrue(document.contains("pig"));
        assertTrue(document.contains("item subsets"));
        assertTrue(document.contains("searchfield"));
        assertTrue(document.contains("searchtokenparser"));
        assertTrue(document.contains("remap"));
        assertTrue(document.contains("hidden"));
        assertTrue(document.contains("page 2"));
        assertTrue(document.contains("entityitem"));
        assertTrue(document.contains("fertilizeddirt_tilled"));
        assertTrue(document.contains("12/12/10"));
        assertTrue(document.contains("7950l water"));
    }

    @Test
    public void release1123RegressionCoverageKeepsCompatibilityTargetPins() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        assertTrue(source.contains("public static final String TARGET_GTNH = \"2.9.0-beta-2\";"));
        assertTrue(source.contains("public static final String TARGET_NEI = \"2.8.111-GTNH\";"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
