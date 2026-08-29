package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release114ContractTest {

    @Test
    public void runtimeMetadataAndReadmeAgreeOn114() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String mcmod = read("src/main/resources/mcmod.info");
        String readme = read("README.md");

        assertTrue(source.contains("public static final String VERSION = \"1.1.4\";"));
        assertTrue(mcmod.contains("\"version\": \"1.1.4\""));
        assertTrue(readme.contains("Current release: `1.1.4`."));
    }

    @Test
    public void releaseLiveTestCoversIntegrityAndSpeedRegressionMatrix() throws IOException {
        Path path = Paths.get("docs/v1.1.4-live-test.md");
        assertTrue(Files.isRegularFile(path), "missing v1.1.4 live-test checklist");
        if (!Files.isRegularFile(path)) return;

        String document = read(path.toString()).toLowerCase();
        assertContains(document, "persistence");
        assertContains(document, "daybloom");
        assertContains(document, "wand");
        assertContains(document, "manual backup");
        assertContains(document, "automatic backup");
        assertContains(document, "shift+right-click");
        assertContains(document, "stackable");
        assertContains(document, "non-stackable");
        assertContains(document, "command hints");
        assertContains(document, "/journey dump");
        assertContains(document, "2x");
        assertContains(document, "4x");
        assertContains(document, "8x");
        assertContains(document, "gregtech");
        assertContains(document, "botania");
        assertContains(document, "furnace");
        assertContains(document, "fluid");
        assertContains(document, "redstone");
        assertContains(document, "wall-clock");
        assertContains(document, "restart");
        assertContains(document, "1x");
    }

    private static void assertContains(String text, String expected) {
        assertTrue(text.contains(expected), "missing live-test coverage: " + expected);
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
