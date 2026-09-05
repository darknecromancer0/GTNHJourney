package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** Prevents stable Journey sort metadata from silently returning to the per-keystroke rebuild path. */
public class JourneyPrecacheContractTest {

    @Test
    public void nativeIndexesShareTheAtomicItemListGenerationCache() throws IOException {
        String representative = read("src/main/java/dev/gtnhjourney/nei/JourneyNativeRepresentativeIndex.java");
        String family = read("src/main/java/dev/gtnhjourney/nei/JourneyNativeFamilyIndex.java");
        String cache = read("src/main/java/dev/gtnhjourney/nei/JourneyPanelPrecache.java");

        assertTrue(representative.contains("JourneyPanelPrecache.nativeCatalog(nativeItems)"));
        assertTrue(family.contains("JourneyPanelPrecache.nativeCatalog(nativeItems)"));
        assertTrue(cache.contains("source == items"), "catalog validity must stay O(1), not rescan ItemList on every query");
        assertTrue(cache.contains("private static volatile NativeCatalog catalog"));
    }

    @Test
    public void semanticClassificationIsCachedButDynamicJourneyChronologyIsNot() throws IOException {
        String classifier = read("src/main/java/dev/gtnhjourney/nei/JourneySemanticClassifier.java");
        String cache = read("src/main/java/dev/gtnhjourney/nei/JourneyPanelPrecache.java");

        assertTrue(classifier.contains("JourneyPanelPrecache.semantic("));
        assertTrue(cache.contains("ConcurrentHashMap<ResearchKey, SemanticMetadata>"));
        assertFalse(cache.contains("ClientIssuedMirror"));
        assertFalse(cache.contains("ClientActivityMirror"));
        assertFalse(cache.contains("ClientFavouriteMirror"));
        assertFalse(cache.contains("ClientResearchMirror"));
    }

    @Test
    public void clientSessionResetDropsTheRamOnlyPrecache() throws IOException {
        String tracker = read("src/main/java/dev/gtnhjourney/nei/JourneyNEIRefreshTracker.java");
        String cache = read("src/main/java/dev/gtnhjourney/nei/JourneyPanelPrecache.java");

        assertTrue(tracker.contains("JourneyPanelPrecache.clear()"));
        assertFalse(cache.contains("java.io."), "precache must remain RAM-only");
        assertFalse(cache.contains("Files."), "precache must never create an on-disk cache");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
