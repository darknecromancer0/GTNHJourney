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
    public void craftedEmptyTankKeepsGtnhTierSpecificZeroPercentDamage() throws Exception {
        assertEquals(1000, canonicalMeta(LIGHT, 1000));
        assertEquals(2000, canonicalMeta(MEDIUM, 2000));
        assertEquals(4000, canonicalMeta(HEAVY, 4000));
    }

    @Test
    public void anyPositiveOxygenCollapsesToFullEndpoint() throws Exception {
        assertEquals(0, canonicalMeta(LIGHT, 999));
        assertEquals(0, canonicalMeta(LIGHT, 1));
        assertEquals(0, canonicalMeta(MEDIUM, 1999));
        assertEquals(0, canonicalMeta(MEDIUM, 1));
        assertEquals(0, canonicalMeta(HEAVY, 3999));
        assertEquals(0, canonicalMeta(HEAVY, 1));
        assertEquals(0, canonicalMeta(HEAVY, 0));
    }

    @Test
    public void journey119TenPercentEndpointsMigrateToGtnhEmptyWithoutDestroyingFull() throws Exception {
        assertEquals(1000, migratePersistedMeta(LIGHT, 900));
        assertEquals(2000, migratePersistedMeta(MEDIUM, 1800));
        assertEquals(4000, migratePersistedMeta(HEAVY, 2700));

        assertEquals(0, migratePersistedMeta(LIGHT, 0));
        assertEquals(0, migratePersistedMeta(MEDIUM, 0));
        assertEquals(0, migratePersistedMeta(HEAVY, 0));

        assertEquals(1000, PersistedResearchEntryResolver.resolve(LIGHT, 900, "", null).getMeta());
        assertEquals(2000, PersistedResearchEntryResolver.resolve(MEDIUM, 1800, "", null).getMeta());
        assertEquals(4000, PersistedResearchEntryResolver.resolve(HEAVY, 2700, "", null).getMeta());
        assertEquals(0, PersistedResearchEntryResolver.resolve(LIGHT, 0, "", null).getMeta());
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
