package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

class MobSpawnerStatePolicyTest {

    @Test
    void explicitMarkerWinsAndPreservesModdedEntityName() {
        assertEquals(
            "GalacticraftCore.EvolvedCreeper",
            MobSpawnerStatePolicy.resolveEntityName(258, "GalacticraftCore.EvolvedCreeper", id -> "Pig"));
    }

    @Test
    void legacyMetaCanBeUpgradedWithoutRescanning() {
        IntFunction<String> resolver = id -> id == 258 ? "GalacticraftCore.EvolvedCreeper" : null;
        assertEquals("GalacticraftCore.EvolvedCreeper", MobSpawnerStatePolicy.resolveEntityName(258, null, resolver));
    }

    @Test
    void invalidLegacyMetaDoesNotInventPig() {
        assertNull(MobSpawnerStatePolicy.resolveEntityName(0, null, id -> "Pig"));
        assertNull(MobSpawnerStatePolicy.resolveEntityName(9999, null, id -> null));
    }

    @Test
    void placementMarkerIsTransientForResearchIdentity() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(MobSpawnerStatePolicy.ENTITY_TAG, "Creeper");
        tag.setString("kept", "value");

        MobSpawnerStatePolicy.normalizeIdentity("minecraft:mob_spawner", tag);

        assertEquals(false, tag.hasKey(MobSpawnerStatePolicy.ENTITY_TAG));
        assertEquals("value", tag.getString("kept"));
    }
}
