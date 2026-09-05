package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneyNativeFilterInvalidationContractTest {

    @Test
    public void journeyObservesCompletedNativeFilterPublicationRatherThanEveryRestart() throws IOException {
        String legacy = read("src/main/java/dev/gtnhjourney/mixin/RestartableTaskJourneyFilterMixin.java");
        String completion = read("src/main/java/dev/gtnhjourney/mixin/ItemPanelJourneyFilterMixin.java");
        String config = read("src/main/resources/mixins.gtnhjourney.json");

        assertFalse(config.contains("\"RestartableTaskJourneyFilterMixin\""));
        assertFalse(legacy.contains("@Inject(method = \"restart\""));
        assertFalse(legacy.contains("JourneyNeiFilterRevision.invalidate()"));
        assertTrue(completion.contains("@Mixin(value = ItemPanel.class"));
        assertTrue(completion.contains("@Inject(method = \"updateItemList\""));
        assertTrue(completion.contains("JourneyPanelController.captureCompletedNativeFilter"));
        assertTrue(config.contains("\"ItemPanelJourneyFilterMixin\""));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
