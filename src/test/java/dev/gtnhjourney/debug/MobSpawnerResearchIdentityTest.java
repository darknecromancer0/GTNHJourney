package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class MobSpawnerResearchIdentityTest {

    @Test
    public void vanillaAndModdedEntityNamesResolveToExactSpawnerMetadata() {
        Map<String, Integer> ids = new HashMap<String, Integer>();
        ids.put("Spider", Integer.valueOf(52));
        ids.put("CaveSpider", Integer.valueOf(59));
        ids.put("SpecialMobs.SwarmSpider", Integer.valueOf(731));

        assertEquals(52, MobSpawnerResearchIdentity.resolveEntityMeta("Spider", ids));
        assertEquals(59, MobSpawnerResearchIdentity.resolveEntityMeta("CaveSpider", ids));
        assertEquals(731, MobSpawnerResearchIdentity.resolveEntityMeta("SpecialMobs.SwarmSpider", ids));
    }

    @Test
    public void missingOrZeroEntityMappingNeverFallsBackToPig() {
        Map<String, Integer> ids = new HashMap<String, Integer>();
        ids.put("BrokenEntity", Integer.valueOf(0));

        assertEquals(-1, MobSpawnerResearchIdentity.resolveEntityMeta("BrokenEntity", ids));
        assertEquals(-1, MobSpawnerResearchIdentity.resolveEntityMeta("UnknownEntity", ids));
        assertEquals(-1, MobSpawnerResearchIdentity.resolveEntityMeta("", ids));
        assertEquals(-1, MobSpawnerResearchIdentity.resolveEntityMeta(null, ids));
    }

    @Test
    public void onlyLegacyUntypedVanillaSpawnerIsDiscarded() {
        assertTrue(MobSpawnerResearchIdentity.isLegacyUntypedSpawner("minecraft:mob_spawner", 0, ""));
        assertFalse(MobSpawnerResearchIdentity.isLegacyUntypedSpawner("minecraft:mob_spawner", 52, ""));
        assertFalse(MobSpawnerResearchIdentity.isLegacyUntypedSpawner("minecraft:mob_spawner", 59, ""));
        assertFalse(MobSpawnerResearchIdentity.isLegacyUntypedSpawner("minecraft:mob_spawner", 0, "10{8:EntityId=8:\"Spider\";}"));
        assertFalse(MobSpawnerResearchIdentity.isLegacyUntypedSpawner("EnderIO:itemBrokenSpawner", 0, ""));
    }
}
