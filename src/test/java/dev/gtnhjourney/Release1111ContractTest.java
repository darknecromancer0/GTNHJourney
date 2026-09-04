package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release1111ContractTest {

    @Test
    public void runtimeMetadataAndJourneyPanelIsolationAgreeOnCurrentRelease() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String mcmod = read("src/main/resources/mcmod.info");
        String build = read("build.gradle.kts");
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/ItemsPanelGridJourneyMixin.java");
        String mixins = read("src/main/resources/mixins.gtnhjourney.json");

        assertTrue(source.contains("public static final String VERSION = \"1.1.21\";"));
        assertTrue(mcmod.contains("\"version\": \"1.1.21\""));
        assertTrue(build.contains("version = \"1.1.21\""));
        assertTrue(mixin.contains("CollapsibleItems;isEmpty()Z"));
        assertTrue(mixin.contains("JourneyViewState.isEnabled()"));
        assertTrue(mixins.contains("\"ItemsPanelGridJourneyMixin\""));
    }

    @Test
    public void release1111HasIndustrialDiamondLiveRegressionChecklist() throws IOException {
        Path path = Paths.get("docs/v1.1.11-live-test.md");
        assertTrue(Files.isRegularFile(path), "missing v1.1.11 live-test checklist");
        String document = read(path.toString()).toLowerCase();
        assertTrue(document.contains("ic2:itempartindustrialdiamond"));
        assertTrue(document.contains("minecraft:diamond"));
        assertTrue(document.contains("journey"));
        assertTrue(document.contains("recipe"));
        assertTrue(document.contains("restart"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
