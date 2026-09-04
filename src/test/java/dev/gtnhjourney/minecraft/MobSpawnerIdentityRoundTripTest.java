package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class MobSpawnerIdentityRoundTripTest {

    private static final int SILVERFISH_ENTITY_ID = 60;

    @Test
    public void positiveSpawnerEntityMetadataSurvivesCanonicalizationAndPersistedLoad() {
        assertEquals(
            SILVERFISH_ENTITY_ID,
            VanillaMetadataPolicy.canonicalMeta("minecraft:mob_spawner", SILVERFISH_ENTITY_ID));

        PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver.resolveEntry(
            "minecraft:mob_spawner",
            SILVERFISH_ENTITY_ID,
            "",
            null);

        assertNotNull(resolved);
        assertEquals("minecraft:mob_spawner", resolved.key().getItemId());
        assertEquals(SILVERFISH_ENTITY_ID, resolved.key().getMeta());
    }

    @Test
    public void legacyUntypedSpawnerNeverBecomesPigFallback() {
        assertNull(PersistedResearchEntryResolver.resolveEntry("minecraft:mob_spawner", 0, "", null));
    }

    @Test
    public void retrievalFactoryReconstructsExactPersistedMetadata() throws IOException {
        String factory = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/retrieval/ItemStackTemplateFactory.java")),
            StandardCharsets.UTF_8);
        assertTrue(factory.contains("new ItemStack(item, 1, key.getMeta())"));
    }
}
