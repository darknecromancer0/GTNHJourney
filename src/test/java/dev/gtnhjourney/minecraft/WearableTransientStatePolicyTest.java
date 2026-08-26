package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class WearableTransientStatePolicyTest {

    @Test
    public void emtQuantumChestplateDropsOnlyObservedZeroRuntimeFlags() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("unequip", (byte) 0);
        tag.setByte("wing", (byte) 0);
        tag.setString("PersistentUpgrade", "keep");

        WearableTransientStatePolicy.normalize("EMT:itemArmorQuantumChestplate", tag);

        assertFalse(tag.hasKey("unequip"));
        assertFalse(tag.hasKey("wing"));
        assertEquals("keep", tag.getString("PersistentUpgrade"));
    }

    @Test
    public void emtNonZeroRuntimeFlagsStayExact() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("unequip", (byte) 1);
        tag.setByte("wing", (byte) 1);

        WearableTransientStatePolicy.normalize("EMT:itemArmorQuantumChestplate", tag);

        assertTrue(tag.hasKey("unequip"));
        assertTrue(tag.hasKey("wing"));
        assertEquals(1, tag.getByte("unequip"));
        assertEquals(1, tag.getByte("wing"));
    }

    @Test
    public void wyvernChestDropsOnlyRuntimeShieldFields() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Energy", 1000000);
        tag.setFloat("ProtectionPoints", 80.0F);
        tag.setFloat("ShieldEntropy", 0.25F);
        tag.setString("PersistentUpgrade", "keep");

        WearableTransientStatePolicy.normalize("DraconicEvolution:wyvernChest", tag);

        assertFalse(tag.hasKey("ProtectionPoints"));
        assertFalse(tag.hasKey("ShieldEntropy"));
        assertEquals(1000000, tag.getInteger("Energy"));
        assertEquals("keep", tag.getString("PersistentUpgrade"));
    }

    @Test
    public void foreignLookalikeNbtRemainsExact() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("unequip", (byte) 0);
        tag.setByte("wing", (byte) 0);
        tag.setFloat("ProtectionPoints", 80.0F);
        tag.setFloat("ShieldEntropy", 0.25F);

        WearableTransientStatePolicy.normalize("test:foreignArmor", tag);

        assertTrue(tag.hasKey("unequip"));
        assertTrue(tag.hasKey("wing"));
        assertTrue(tag.hasKey("ProtectionPoints"));
        assertTrue(tag.hasKey("ShieldEntropy"));
    }
}
