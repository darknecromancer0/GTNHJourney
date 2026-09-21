package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class DuplicateStateNormalizationBehaviorTest {

    @Test
    public void ae2ItemCellPayloadIsRemovedButConfigurationSurvives() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("#0", new NBTTagCompound());
        tag.setLong("@0", 64L);
        tag.setShort("it", (short) 1);
        tag.setLong("ic", 64L);
        tag.setString("oreFilter", "dustIron");

        EmbeddedInventoryPolicy.normalize("appliedenergistics2:item.ItemBasicStorageCell.4k", tag);

        assertFalse(tag.hasKey("#0"));
        assertFalse(tag.hasKey("@0"));
        assertFalse(tag.hasKey("it"));
        assertFalse(tag.hasKey("ic"));
        assertEquals("dustIron", tag.getString("oreFilter"));
    }

    @Test
    public void ae2fcFluidCellPayloadIsRemoved() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", "nitrofuel");
        fluid.setLong("Cnt", 8372224L);
        tag.setTag("#0", fluid);
        tag.setLong("@0", 8372224L);
        tag.setShort("ft", (short) 1);
        tag.setLong("fc", 8372224L);

        EmbeddedInventoryPolicy.normalize("ae2fc:fluid_storage4", tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void ultraTerminalDropsCraftingCacheButKeepsBinding() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("crafting", new NBTTagCompound());
        tag.setInteger("MagnetMode", 0);
        tag.setString("name", "");
        tag.setString("encryptionKey", "-1235384731");
        NBTTagCompound keys = new NBTTagCompound();
        keys.setString("White", "-1235384731");
        tag.setTag("encryptionKeys", keys);

        KnownTransientItemStatePolicy.normalize("ae2fc:wireless_ultra_terminal", 0, tag);

        assertFalse(tag.hasKey("crafting"));
        assertFalse(tag.hasKey("MagnetMode"));
        assertFalse(tag.hasKey("name"));
        assertEquals("-1235384731", tag.getString("encryptionKey"));
        assertTrue(tag.hasKey("encryptionKeys", 10));
    }

    @Test
    public void clipboardWorkPagesCollapseToBase() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("currentPage", 2);
        tag.setInteger("totalPages", 4);
        tag.setTag("page1", new NBTTagCompound());
        tag.setTag("page4", new NBTTagCompound());

        KnownTransientItemStatePolicy.normalize("BiblioCraft:item.BiblioClipboard", 0, tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void graviFlightFlagsDisappearButChargeSurvives() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("charge", 3000000.0D);
        tag.setBoolean("isFlyActive", true);
        tag.setBoolean("isHoverActive", true);
        tag.setByte("toggleTimer", (byte) 11);

        KnownTransientItemStatePolicy.normalize("GraviSuite:advJetpack", 1, tag);

        assertEquals(3000000.0D, tag.getDouble("charge"));
        assertFalse(tag.hasKey("isFlyActive"));
        assertFalse(tag.hasKey("isHoverActive"));
        assertFalse(tag.hasKey("toggleTimer"));
    }

    @Test
    public void portableScannerDropsCachedScanAndPollutionLinesButKeepsCharge() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("GT.ItemCharge", 400000L);
        tag.setInteger("dataLinesCount", 6);
        tag.setTag("scanLine0", new NBTTagCompound());
        tag.setTag("scanLine5", new NBTTagCompound());

        KnownTransientItemStatePolicy.normalize("gregtech:gt.metaitem.01", 32762, tag);

        assertEquals(400000L, tag.getLong("GT.ItemCharge"));
        assertFalse(tag.hasKey("dataLinesCount"));
        assertFalse(tag.hasKey("scanLine0"));
        assertFalse(tag.hasKey("scanLine5"));
    }

    @Test
    public void itemDislocatorProfilesCollapseToBase() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList profiles = new NBTTagList();
        NBTTagCompound enabled = new NBTTagCompound();
        enabled.setBoolean("Enabled", true);
        profiles.appendTag(enabled);
        tag.setTag("ConfigProfiles", profiles);

        KnownTransientItemStatePolicy.normalize("DraconicEvolution:magnet", 0, tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void taintedVillagerSoulVialDropsQuotedEmptyNameAndPerEntityRuntime() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "Thaumcraft.TaintedVillager");
        tag.setString("CustomName", "\"\"");
        tag.setLong("UUIDMost", 11L);
        tag.setLong("UUIDLeast", 12L);
        tag.setDouble("Health", 26.0D);
        tag.setTag("Attributes", new NBTTagList());
        tag.setTag("CreatureInfusion", new NBTTagCompound());
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "\"\"");
        tag.setTag("display", display);

        KnownTransientItemStatePolicy.normalize("EnderIO:itemSoulVessel", 0, tag);

        assertEquals("Thaumcraft.TaintedVillager", tag.getString("id"));
        assertEquals(1, tag.func_150296_c().size());
        assertFalse(tag.hasKey("CustomName"));
        assertFalse(tag.hasKey("display"));
    }

    @Test
    public void gtToolboxRuntimeAndContentsCollapseToEmptyToolbox() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound contents = new NBTTagCompound();
        contents.setInteger("Size", 14);
        tag.setTag("gt5u.toolbox:Contents", contents);
        tag.setBoolean("gt5u.toolbox:ToolboxOpen", true);

        KnownTransientItemStatePolicy.normalize("gregtech:gt.Item_Toolbox", 0, tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void detravScannerModeIsRuntimeButCoreToolStatsSurvive() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound stats = new NBTTagCompound();
        stats.setLong("DetravData", 3L);
        stats.setLong("MaxCharge", 102400000L);
        tag.setTag("GT.ToolStats", stats);

        KnownTransientItemStatePolicy.normalize("gregtech:gt.detrav.metatool.01", 100, tag);

        assertFalse(tag.getCompoundTag("GT.ToolStats").hasKey("DetravData"));
        assertEquals(102400000L, tag.getCompoundTag("GT.ToolStats").getLong("MaxCharge"));
    }

    @Test
    public void enderIoMachineEnergyDoesNotCreateResearchVariants() {
        NBTTagCompound charger = new NBTTagCompound();
        charger.setInteger("storedEnergyRF", 200000);
        KnownTransientItemStatePolicy.normalize("EnderIO:blockWirelessCharger", 0, charger);
        assertTrue(charger.func_150296_c().isEmpty());

        NBTTagCompound capBank = new NBTTagCompound();
        capBank.setInteger("storedEnergyRF", 25000000);
        capBank.setString("type", "VIBRANT");
        KnownTransientItemStatePolicy.normalize("EnderIO:blockCapBank", 3, capBank);
        assertFalse(capBank.hasKey("storedEnergyRF"));
        assertEquals("VIBRANT", capBank.getString("type"));
    }

    @Test
    public void vanillaEquipmentEnchantmentsCollapseButCustomDisplaySurvives() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("ench", new NBTTagList());
        tag.setInteger("RepairCost", 7);
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Tempered Blade");
        tag.setTag("display", display);

        VanillaEquipmentStatePolicy.normalize("minecraft:iron_sword", tag);

        assertFalse(tag.hasKey("ench"));
        assertFalse(tag.hasKey("RepairCost"));
        assertEquals("Tempered Blade", tag.getCompoundTag("display").getString("Name"));
    }

    @Test
    public void invalidBopHiveMetaCanonicalizesToHoneycomb() {
        assertEquals(0, KnownMetadataAliasPolicy.canonicalMeta("BiomesOPlenty:hive", 9));
        assertEquals(2, KnownMetadataAliasPolicy.canonicalMeta("BiomesOPlenty:hive", 2));
        assertEquals(9, KnownMetadataAliasPolicy.canonicalMeta("minecraft:wool", 9));
    }

    @Test
    public void experienceObeliskXpSnapshotCollapsesToCleanItem() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Items", new NBTTagList());
        tag.setBoolean("eio.abstractMachine", true);
        tag.setFloat("experience", 0.8037808F);
        tag.setInteger("experienceLevel", 142);
        tag.setInteger("experienceTotal", 51961);
        tag.setInteger("redstoneControlMode", 0);
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Experience Obelisk (Configured)");
        tag.setTag("display", display);

        KnownTransientItemStatePolicy.normalize("EnderIO:blockExperienceObelisk", 0, tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void defaultConfiguredStirlingGeneratorCollapsesToBase() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Items", new NBTTagList());
        tag.setShort("capacitorType", (short) 0);
        tag.setBoolean("eio.abstractMachine", true);
        tag.setInteger("redstoneControlMode", 0);
        tag.setInteger("storedEnergyRF", 0);
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Stirling Generator (Configured)");
        tag.setTag("display", display);

        KnownTransientItemStatePolicy.normalize("EnderIO:blockStirlingGenerator", 0, tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void enhancedLootBagVisualEnchantStateCollapses() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("ench", new NBTTagList());
        tag.setInteger("RepairCost", 2);

        KnownTransientItemStatePolicy.normalize("enhancedlootbags:lootbag", 4, tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void defaultRailcraftTankColorDoesNotCreateDuplicate() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("color", 15);

        KnownTransientItemStatePolicy.normalize("Railcraft:machine.zeta", 9, tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void disabledCleansingTalismanStateCollapsesToBase() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("enabled", 0L);

        KnownTransientItemStatePolicy.normalize("ThaumicTinkerer:cleansingTalisman", 0, tag);

        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void thaumcraftJarAmountBecomesFullWithoutLosingAspectOrFilter() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("AspectFilter", "terra");
        NBTTagCompound aspect = new NBTTagCompound();
        aspect.setString("key", "terra");
        aspect.setInteger("amount", 14);
        NBTTagList aspects = new NBTTagList();
        aspects.appendTag(aspect);
        tag.setTag("Aspects", aspects);

        ThaumcraftJarStatePolicy.normalize("Thaumcraft:BlockJarFilledItem", tag);

        assertEquals("terra", tag.getString("AspectFilter"));
        assertEquals("terra", tag.getTagList("Aspects", 10).getCompoundTagAt(0).getString("key"));
        assertEquals(64, tag.getTagList("Aspects", 10).getCompoundTagAt(0).getInteger("amount"));
    }

    @Test
    public void galaxyJetplatePersistedChargeHasOnlyEmptyOrFullEndpoints() {
        NBTTagCompound partial = new NBTTagCompound();
        partial.setFloat("electricity", 99665.0F);
        GalaxySpaceChargeStatePolicy.normalizePersisted("GalaxySpace:item.spacesuit_jetplate", 100, partial);
        assertEquals(100000.0F, partial.getFloat("electricity"));

        NBTTagCompound empty = new NBTTagCompound();
        empty.setFloat("electricity", 0.0F);
        GalaxySpaceChargeStatePolicy.normalizePersisted("GalaxySpace:item.spacesuit_jetplate", 100, empty);
        assertFalse(empty.hasKey("electricity"));
    }
}
