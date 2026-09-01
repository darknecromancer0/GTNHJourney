package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class PersistedDirtMetadataMigrationTest {

    @Test
    public void oldDefaultDirtMetadataMigratesToCanonicalZeroWithoutRegistryReconstruction() {
        assertMetaMigrates(1, 0);
        assertMetaMigrates(7, 0);
        assertMetaMigrates(49, 0);
    }

    @Test
    public void podzolMetadataRemainsDistinct() {
        assertMetaMigrates(2, 2);
    }

    private static void assertMetaMigrates(int oldMeta, int expectedMeta) {
        ResearchKey key = PersistedResearchEntryResolver.resolve("minecraft:dirt", oldMeta, "", null);
        assertNotNull(key);
        assertEquals(expectedMeta, key.getMeta());
    }
}
