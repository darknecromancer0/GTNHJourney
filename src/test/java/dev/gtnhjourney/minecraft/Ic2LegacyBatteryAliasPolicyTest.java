package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class Ic2LegacyBatteryAliasPolicyTest {

    @Test
    public void nativeDischargedRegistryIdentityIsNoLongerAliased() {
        assertEquals(
            "IC2:itemBatREDischarged",
            Ic2LegacyBatteryAliasPolicy.canonicalItemId("IC2:itemBatREDischarged"));
        assertEquals("IC2:itemBatRE", Ic2LegacyBatteryAliasPolicy.canonicalItemId("IC2:itemBatRE"));
        assertEquals("IC2:itemBatBox", Ic2LegacyBatteryAliasPolicy.canonicalItemId("IC2:itemBatBox"));
    }

    @Test
    public void persistedBadEmptyBatteryMigratesBackToCraftedRegistryItem() {
        assertEquals(
            "IC2:itemBatREDischarged",
            Ic2LegacyBatteryAliasPolicy.migratePersistedItemId("IC2:itemBatRE", 27, null));
        assertEquals(0, Ic2LegacyBatteryAliasPolicy.migratePersistedMeta("IC2:itemBatRE", 27, null));
    }

    @Test
    public void persistedChargedBatteryKeepsRechargeableRegistryItem() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("charge", 10000.0D);

        assertEquals(
            "IC2:itemBatRE",
            Ic2LegacyBatteryAliasPolicy.migratePersistedItemId("IC2:itemBatRE", 1, tag));
        assertEquals(1, Ic2LegacyBatteryAliasPolicy.migratePersistedMeta("IC2:itemBatRE", 1, tag));
    }

    @Test
    public void itemStackKeyFactoryNormalizesSplitBatteryBeforeGenericChargeSemantics() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/ItemStackKeyFactory.java");
        int option = source.indexOf("ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()");
        int splitBattery = source.indexOf("Ic2LegacyBatteryAliasPolicy.identityStack(stack)");
        int charge = source.indexOf("Ic2ChargeStatePolicy.classify(canonicalInput)");
        assertTrue(option >= 0);
        assertTrue(splitBattery > option);
        assertTrue(charge > splitBattery);
    }

    @Test
    public void stateExpanderHandlesSplitBatteryBeforeGenericIc2Expansion() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/ResearchStateExpander.java");
        int splitBattery = source.indexOf("Ic2LegacyBatteryAliasPolicy.expand(exact)");
        int charge = source.indexOf("Ic2ChargeStatePolicy.classify(exact)");
        assertTrue(splitBattery >= 0);
        assertTrue(charge > splitBattery);
    }

    @Test
    public void persistedMigrationUsesSplitBatteryEndpointMigration() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/minecraft/PersistedResearchEntryResolver.java");
        int option = source.indexOf("ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()");
        int itemMigration = source.indexOf("Ic2LegacyBatteryAliasPolicy.migratePersistedItemId");
        int metaMigration = source.indexOf("Ic2LegacyBatteryAliasPolicy.migratePersistedMeta");
        assertTrue(option >= 0);
        assertTrue(itemMigration > option);
        assertTrue(metaMigration > itemMigration);
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
