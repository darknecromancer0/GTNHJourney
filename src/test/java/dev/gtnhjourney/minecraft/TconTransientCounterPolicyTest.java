package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class TconTransientCounterPolicyTest {

    @Test
    public void knownIguanaExtraCountersAreTransientButRealModifierKeysAreNot() throws Exception {
        Method classifier = TconToolStatePolicy.class.getDeclaredMethod("isTransientIguanaCounterKey", String.class);
        classifier.setAccessible(true);

        assertTrue((Boolean) classifier.invoke(null, "ExtraRedstone"));
        assertTrue((Boolean) classifier.invoke(null, "ExtraLuckLooting"));
        assertTrue((Boolean) classifier.invoke(null, "ExtraCritical"));
        assertFalse((Boolean) classifier.invoke(null, "Redstone"));
        assertFalse((Boolean) classifier.invoke(null, "Modifiers"));
        assertFalse((Boolean) classifier.invoke(null, "ExtraCustomData"));
    }

    @Test
    public void consumableAmmoIsRestoredToItsFullEndpoint() throws Exception {
        Method fullAmmo = TconToolStatePolicy.class.getDeclaredMethod("fullAmmoForDurability", int.class);
        fullAmmo.setAccessible(true);
        assertEquals(30, ((Integer) fullAmmo.invoke(null, 300)).intValue());
        assertEquals(29, ((Integer) fullAmmo.invoke(null, 285)).intValue());
        assertEquals(19, ((Integer) fullAmmo.invoke(null, 188)).intValue());

        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound infiTool = new NBTTagCompound();
        infiTool.setInteger("TotalDurability", 285);
        infiTool.setInteger("Ammo", 3);
        infiTool.setInteger("RepairCount", 5);
        tag.setTag("InfiTool", infiTool);

        Method normalizeAmmo = TconToolStatePolicy.class.getDeclaredMethod("normalizeAmmoState", NBTTagCompound.class);
        normalizeAmmo.setAccessible(true);
        normalizeAmmo.invoke(null, tag);

        assertEquals(29, tag.getCompoundTag("InfiTool").getInteger("Ammo"));
        assertEquals(5, tag.getCompoundTag("InfiTool").getInteger("RepairCount"));
    }
}
