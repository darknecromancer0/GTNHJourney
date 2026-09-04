package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class Release1111ContractTest {

    private static final Pattern RUNTIME_VERSION = Pattern.compile(
        "public static final String VERSION = \\\"([^\\\"]+)\\\";");

    @Test
    public void runtimeMetadataAndJourneyPanelIsolationAgreeOnCurrentRelease() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String mcmod = read("src/main/resources/mcmod.info");
        String build = read("build.gradle.kts");
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/ItemsPanelGridJourneyMixin.java");
        String mixins = read("src/main/resources/mixins.gtnhjourney.json");

        Matcher matcher = RUNTIME_VERSION.matcher(source);
        assertTrue(matcher.find(), "runtime version constant missing");
        String version = matcher.group(1);
        assertTrue(mcmod.contains("\"version\": \"" + version + "\""));
        assertTrue(build.contains("version = \"" + version + "\""));
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
