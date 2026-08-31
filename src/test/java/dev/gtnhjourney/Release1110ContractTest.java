package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release1110ContractTest {

    @Test
    public void reBatteryMigrationFixRemainsPresent() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String aliases = read("src/main/java/dev/gtnhjourney/minecraft/Ic2LegacyBatteryAliasPolicy.java");
        String resolver = read("src/main/java/dev/gtnhjourney/minecraft/PersistedResearchEntryResolver.java");
        String keyFactory = read("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java");

        assertTrue(aliases.contains("IC2:itemBatREDischarged"));
        assertTrue(aliases.contains("IC2:itemBatRE"));
        assertTrue(resolver.contains("canonicalItemId(itemId)"));
        assertTrue(keyFactory.contains("identityStack(stack)"));
        assertTrue(source.contains("JourneyResearchData.get(rootWorld)"));
        assertTrue(source.contains("markDirty()"));
    }

    @Test
    public void release1110HasLiveRegressionChecklist() throws IOException {
        Path path = Paths.get("docs/v1.1.10-live-test.md");
        assertTrue(Files.isRegularFile(path), "missing v1.1.10 live-test checklist");
        String document = read(path.toString()).toLowerCase();
        assertTrue(document.contains("itembatredischarged"));
        assertTrue(document.contains("itembatre"));
        assertTrue(document.contains("0 / 10 k eu"));
        assertTrue(document.contains("restart"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
