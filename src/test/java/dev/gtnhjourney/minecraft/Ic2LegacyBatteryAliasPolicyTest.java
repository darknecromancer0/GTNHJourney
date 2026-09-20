package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class Ic2LegacyBatteryAliasPolicyTest {

    @Test
    public void onlyKnownDischargedReBatteryAliasChangesRegistryIdentity() {
        assertEquals("IC2:itemBatRE", Ic2LegacyBatteryAliasPolicy.canonicalItemId("IC2:itemBatREDischarged"));
        assertEquals("IC2:itemBatRE", Ic2LegacyBatteryAliasPolicy.canonicalItemId("IC2:itemBatRE"));
        assertEquals("IC2:itemBatBox", Ic2LegacyBatteryAliasPolicy.canonicalItemId("IC2:itemBatBox"));
    }

    @Test
    public void onlyCanonicalEmptyRechargeableEndpointUsesCraftedPhysicalRepresentation() {
        assertTrue(Ic2LegacyBatteryAliasPolicy.isCanonicalEmptyEndpoint("IC2:itemBatRE", 27, null));
        assertFalse(Ic2LegacyBatteryAliasPolicy.isCanonicalEmptyEndpoint("IC2:itemBatRE", 1, null));
        assertFalse(Ic2LegacyBatteryAliasPolicy.isCanonicalEmptyEndpoint("IC2:itemBatREDischarged", 0, null));

        NBTTagCompound charged = new NBTTagCompound();
        charged.setDouble("charge", 10000.0D);
        assertFalse(Ic2LegacyBatteryAliasPolicy.isCanonicalEmptyEndpoint("IC2:itemBatRE", 27, charged));
    }

    @Test
    public void itemStackKeyFactoryCanonicalizesAliasBeforeChargeSemantics() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java");
        int option = source.indexOf("ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()");
        int alias = source.indexOf("Ic2LegacyBatteryAliasPolicy.identityStack(stack)");
        int charge = source.indexOf("Ic2ChargeStatePolicy.classify(canonicalInput)");
        assertTrue(option >= 0);
        assertTrue(alias > option);
        assertTrue(charge > alias);
    }

    @Test
    public void retrievalFactoryRecreatesNativeCraftedBatteryBeforeGenericLookup() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/retrieval/ItemStackTemplateFactory.java");
        int nativeBattery = source.indexOf("Ic2LegacyBatteryAliasPolicy");
        int nativeCall = source.indexOf(".nativeRetrievalStack(");
        int genericLookup = source.indexOf("GameRegistry.findItem(modId, name)");
        assertTrue(nativeBattery >= 0);
        assertTrue(nativeCall > nativeBattery);
        assertTrue(genericLookup > nativeCall);
    }

    @Test
    public void persistedAliasMigrationAlsoHonorsIc2CompatibilityOption() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/PersistedResearchEntryResolver.java");
        int option = source.indexOf("ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()");
        int alias = source.indexOf("Ic2LegacyBatteryAliasPolicy.canonicalItemId(itemId)");
        assertTrue(option >= 0);
        assertTrue(alias > option);
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
