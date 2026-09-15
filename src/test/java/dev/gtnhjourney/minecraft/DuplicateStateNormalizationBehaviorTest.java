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
