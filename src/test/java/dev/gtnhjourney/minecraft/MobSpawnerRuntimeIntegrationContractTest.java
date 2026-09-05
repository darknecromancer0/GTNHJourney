package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class MobSpawnerRuntimeIntegrationContractTest {

    @Test
    void legacySpawnerTemplatesAreUpgradedAndPlacedEntityIsAppliedServerSide() throws IOException {
        String factory = read("src/main/java/dev/gtnhjourney/retrieval/ItemStackTemplateFactory.java");
        String identity = read("src/main/java/dev/gtnhjourney/minecraft/ResearchNbtIdentity.java");
        String handler = read("src/main/java/dev/gtnhjourney/acquisition/MobSpawnerPlacementHandler.java");
        String mod = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");

        assertTrue(factory.contains("MobSpawnerStatePolicy.ensurePlacementMarker(stack)"));
        assertTrue(identity.contains("MobSpawnerStatePolicy.normalizeIdentity(canonicalItemId, identityTag)"));
        assertTrue(handler.contains("BlockEvent.PlaceEvent"));
        assertTrue(handler.contains("event.itemInHand"));
        assertTrue(handler.contains("MobSpawnerStatePolicy.applyPlacedSpawner"));
        assertTrue(mod.contains("MinecraftForge.EVENT_BUS.register(MOB_SPAWNER_PLACEMENT)"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
