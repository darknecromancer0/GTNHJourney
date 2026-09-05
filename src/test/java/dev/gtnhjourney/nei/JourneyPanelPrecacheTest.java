package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class JourneyPanelPrecacheTest {

    @AfterEach
    public void clearCache() {
        JourneyPanelPrecache.clear();
    }

    @Test
    public void reusesNativeCatalogForSameImmutableNeiListReference() {
        List<ItemStack> items = new ArrayList<ItemStack>();
        JourneyPanelPrecache.NativeCatalog first = JourneyPanelPrecache.nativeCatalog(items);
        JourneyPanelPrecache.NativeCatalog second = JourneyPanelPrecache.nativeCatalog(items);
        assertSame(first, second);
    }

    @Test
    public void replacingNeiListInvalidatesNativeAndSemanticCachesEvenWhenContentsAreEquivalent() {
        List<ItemStack> firstItems = new ArrayList<ItemStack>();
        List<ItemStack> replacement = new ArrayList<ItemStack>();
        AtomicInteger computations = new AtomicInteger();
        ResearchKey key = new ResearchKey("example:item", 0, "");

        JourneyPanelPrecache.NativeCatalog firstCatalog = JourneyPanelPrecache.nativeCatalog(firstItems);
        JourneyPanelPrecache.SemanticMetadata first = JourneyPanelPrecache.semanticForTest(
            key,
            "Example",
            computer(computations));

        JourneyPanelPrecache.NativeCatalog secondCatalog = JourneyPanelPrecache.nativeCatalog(replacement);
        JourneyPanelPrecache.SemanticMetadata second = JourneyPanelPrecache.semanticForTest(
            key,
            "Example",
            computer(computations));

        assertNotSame(firstCatalog, secondCatalog);
        assertNotSame(first, second);
        assertEquals(2, computations.get());
    }

    @Test
    public void semanticMetadataIsMemoizedForStableIdentityAndDisplayName() {
        JourneyPanelPrecache.nativeCatalog(new ArrayList<ItemStack>());
        AtomicInteger computations = new AtomicInteger();
        ResearchKey key = new ResearchKey("example:item", 7, "{Mode:1b}");

        JourneyPanelPrecache.SemanticMetadata first = JourneyPanelPrecache.semanticForTest(
            key,
            "Example",
            computer(computations));
        JourneyPanelPrecache.SemanticMetadata second = JourneyPanelPrecache.semanticForTest(
            key,
            "Example",
            computer(computations));

        assertSame(first, second);
        assertEquals(1, computations.get());
    }

    @Test
    public void changedDisplayNameRecomputesSemanticMetadataForLanguageOrPresentationChanges() {
        JourneyPanelPrecache.nativeCatalog(new ArrayList<ItemStack>());
        AtomicInteger computations = new AtomicInteger();
        ResearchKey key = new ResearchKey("example:item", 0, "");

        JourneyPanelPrecache.SemanticMetadata first = JourneyPanelPrecache.semanticForTest(
            key,
            "English Name",
            computer(computations));
        JourneyPanelPrecache.SemanticMetadata second = JourneyPanelPrecache.semanticForTest(
            key,
            "Localized Name",
            computer(computations));

        assertNotSame(first, second);
        assertEquals(2, computations.get());
    }

    private static JourneyPanelPrecache.SemanticComputer computer(final AtomicInteger computations) {
        return new JourneyPanelPrecache.SemanticComputer() {
            @Override
            public JourneyPanelPrecache.SemanticMetadata compute(ResearchKey key, String displayName) {
                int call = computations.incrementAndGet();
                return new JourneyPanelPrecache.SemanticMetadata(displayName, "mod" + call, "type", "kind");
            }
        };
    }
}
