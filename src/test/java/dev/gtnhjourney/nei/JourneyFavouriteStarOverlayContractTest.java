package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyFavouriteStarOverlayContractTest {

    @Test
    void drawsAfterCachedItemGridInAllModesAndUsesExactSyncedFavourites() throws Exception {
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/ItemsPanelGridJourneyMixin.java");
        String overlay = read("src/main/java/dev/gtnhjourney/nei/JourneyFavouriteStarOverlay.java");
        assertTrue(mixin.contains("JourneyFavouriteStarOverlay.render("));
        assertTrue(mixin.contains("@Inject(method = \"draw\", at = @At(\"RETURN\"), remap = false)"));
        assertTrue(overlay.contains("ClientFavouriteMirror.contains(ResearchFingerprint.of(key))"));
        assertTrue(overlay.contains("JourneyPresentationKeyResolver.keyOf(stack)"));
        assertTrue(overlay.contains("GL11.glPushAttrib"));
        assertTrue(overlay.contains("GL11.glPopAttrib"));
        assertTrue(!overlay.contains("JourneyViewState.isFavourite()"), "stars must work on every Journey tab");
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
