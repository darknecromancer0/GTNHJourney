package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class GalacticraftOxygenTankStatePolicyTest {

    private static final String LIGHT = "GalacticraftCore:item.oxygenTankLightFull";
    private static final String MEDIUM = "GalacticraftCore:item.oxygenTankMedFull";
    private static final String HEAVY = "GalacticraftCore:item.oxygenTankHeavyFull";

    @Test
    public void craftedEmptyTankKeepsItsTierSpecificEmptyDamage() throws Exception {
        assertEquals(900, canonicalMeta(LIGHT, 900));
        assertEquals(1800, canonicalMeta(MEDIUM, 1800));
        assertEquals(2700, canonicalMeta(HEAVY, 2700));
    }

    @Test
    public void anyPositiveOxygenCollapsesToFullEndpoint() throws Exception {
        assertEquals(0, canonicalMeta(LIGHT, 899));
        assertEquals(0, canonicalMeta(LIGHT, 1));
        assertEquals(0, canonicalMeta(MEDIUM, 1799));
        assertEquals(0, canonicalMeta(MEDIUM, 1));
        assertEquals(0, canonicalMeta(HEAVY, 2699));
        assertEquals(0, canonicalMeta(HEAVY, 1));
        assertEquals(0, canonicalMeta(HEAVY, 0));
    }

    @Test
    public void legacyJourneyFullMetaMigratesBackToEmptyForAllTiers() throws Exception {
        assertEquals(900, migratePersistedMeta(LIGHT, 0));
        assertEquals(1800, migratePersistedMeta(MEDIUM, 0));
        assertEquals(2700, migratePersistedMeta(HEAVY, 0));

        assertEquals(900, PersistedResearchEntryResolver.resolve(LIGHT, 0, "", null).getMeta());
        assertEquals(1800, PersistedResearchEntryResolver.resolve(MEDIUM, 0, "", null).getMeta());
        assertEquals(2700, PersistedResearchEntryResolver.resolve(HEAVY, 0, "", null).getMeta());
    }

    @Test
    public void liveIdentityHandlesOxygenBeforeGenericDurabilityCollapse() throws Exception {
        String source = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java")),
            StandardCharsets.UTF_8);
        int oxygenPolicy = source.indexOf("GalacticraftOxygenTankStatePolicy");
        int durabilityFallback = source.indexOf("isDamageable()");
        assertTrue(oxygenPolicy >= 0, "ItemStackKeyFactory must explicitly recognize Galacticraft oxygen tanks");
        assertTrue(
            durabilityFallback < 0 || oxygenPolicy < durabilityFallback,
            "oxygen fill-state must be canonicalized before generic durability damage is collapsed to zero");
    }

    @Test
    public void unrelatedItemsRemainExact() throws Exception {
        assertEquals(723, canonicalMeta("test:damageable", 723));
        assertEquals(0, migratePersistedMeta("test:damageable", 0));
    }

    private static int canonicalMeta(String itemId, int meta) throws Exception {
        return invoke("canonicalMeta", itemId, meta);
    }

    private static int migratePersistedMeta(String itemId, int meta) throws Exception {
        return invoke("migratePersistedMeta", itemId, meta);
    }

    private static int invoke(String methodName, String itemId, int meta) throws Exception {
        Class<?> policy = Class.forName("dev.gtnhjourney.minecraft.GalacticraftOxygenTankStatePolicy");
        Method method = policy.getMethod(methodName, String.class, int.class);
        return ((Integer) method.invoke(null, itemId, Integer.valueOf(meta))).intValue();
    }
}
