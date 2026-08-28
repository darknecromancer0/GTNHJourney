package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class KnownTransientItemStatePolicyTest {

    @Test
    public void chiselTargetDoesNotDefineResearchIdentity() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound target = new NBTTagCompound();
        target.setShort("id", (short) 4042);
        target.setByte("Count", (byte) 1);
        target.setShort("Damage", (short) 0);
        tag.setTag("chiselTarget", target);
        tag.setString("OwnerLabel", "keep");

        normalize("chisel:chisel", 0, tag);

        assertFalse(tag.hasKey("chiselTarget"));
        assertEquals("keep", tag.getString("OwnerLabel"));
    }

    @Test
    public void buildersWandPlacementHistoryDoesNotDefineResearchIdentity() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound bbw = new NBTTagCompound();
        bbw.setString("lastBlock", "TConstruct:GlassBlock");
        bbw.setInteger("lastDamage", 0);
        bbw.setInteger("lastPerBlock", 1);
        bbw.setIntArray("lastPlaced", new int[] { 489, 82, -26, 490, 82, -26 });
        bbw.setShort("mask", (short) 7);
        bbw.setShort("fluidmask", (short) 1);
        tag.setTag("bbw", bbw);
        tag.setString("displayMarker", "keep");

        normalize("betterbuilderswands:wandDiamond", 0, tag);

        assertFalse(tag.hasKey("bbw"));
        assertEquals("keep", tag.getString("displayMarker"));
    }

    @Test
    public void airFilterWearDoesNotCreateOneStatePerDamagePoint() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound airFilter = new NBTTagCompound();
        airFilter.setLong("Damage", 34L);
        tag.setTag("AirFilter", airFilter);
        tag.setString("Upgrade", "keep");

        normalize("miscutils:itemAirFilter", 0, tag);

        assertFalse(tag.hasKey("AirFilter"));
        assertEquals("keep", tag.getString("Upgrade"));
    }

    @Test
    public void goldenLassoMovementRuntimeDoesNotCreateCopiesOfSameCapturedMob() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "Sheep");
        tag.setLong("UUIDMost", 123L);
        tag.setLong("UUIDLeast", 456L);
        tag.setDouble("FallDistance", 0.0D);
        tag.setInteger("Dimension", 0);
        tag.setShort("Air", (short) 300);
        tag.setShort("Fire", (short) -1);
        tag.setShort("HurtTime", (short) 0);
        tag.setShort("DeathTime", (short) 0);
        tag.setShort("AttackTime", (short) 0);
        tag.setInteger("PortalCooldown", 0);
        tag.setBoolean("OnGround", true);
        tag.setDouble("Health", 8.0D);
        tag.setByte("Color", (byte) 5);
        tag.setTag("Pos", doubles(562.5D, 71.0D, 12.5D));
        tag.setTag("Motion", doubles(0.0D, -0.0784D, 0.0D));
        tag.setTag("Rotation", floats(112.49F, 0.0F));

        normalize("ExtraUtilities:golden_lasso", 1, tag);

        assertEquals("Sheep", tag.getString("id"));
        assertEquals(5, tag.getByte("Color"));
        assertFalse(tag.hasKey("UUIDMost"));
        assertFalse(tag.hasKey("UUIDLeast"));
        assertFalse(tag.hasKey("Pos"));
        assertFalse(tag.hasKey("Motion"));
        assertFalse(tag.hasKey("Rotation"));
        assertFalse(tag.hasKey("OnGround"));
        assertFalse(tag.hasKey("Dimension"));
        assertFalse(tag.hasKey("Health"));
    }

    @Test
    public void unrelatedItemsKeepSameNamedFields() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Damage", 34);
        tag.setLong("UUIDMost", 123L);
        tag.setTag("Pos", doubles(1.0D, 2.0D, 3.0D));

        normalize("example:customItem", 0, tag);

        assertEquals(34, tag.getInteger("Damage"));
        assertEquals(123L, tag.getLong("UUIDMost"));
        assertTrue(tag.hasKey("Pos"));
    }

    private static void normalize(String registryId, int meta, NBTTagCompound tag) throws Exception {
        Class<?> policy = Class.forName("dev.gtnhjourney.minecraft.KnownTransientItemStatePolicy");
        Method normalize = policy.getDeclaredMethod("normalize", String.class, int.class, NBTTagCompound.class);
        normalize.setAccessible(true);
        normalize.invoke(null, registryId, meta, tag);
    }

    private static NBTTagList doubles(double... values) {
        NBTTagList list = new NBTTagList();
        for (double value : values) list.appendTag(new net.minecraft.nbt.NBTTagDouble(value));
        return list;
    }

    private static NBTTagList floats(float... values) {
        NBTTagList list = new NBTTagList();
        for (float value : values) list.appendTag(new net.minecraft.nbt.NBTTagFloat(value));
        return list;
    }
}
