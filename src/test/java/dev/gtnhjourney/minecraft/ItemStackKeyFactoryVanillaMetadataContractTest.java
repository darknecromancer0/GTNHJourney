package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class ItemStackKeyFactoryVanillaMetadataContractTest {

    @Test
    public void newlyObservedStacksUseVanillaMetadataPolicyBeforeBuildingResearchKey() throws IOException {
        String source = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java")),
            StandardCharsets.UTF_8);

        assertTrue(source.contains("VanillaMetadataPolicy.canonicalMeta"));
    }
}
