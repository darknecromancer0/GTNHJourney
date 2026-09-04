package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class KnownTransientWaterStatePolicyTest {

    @Test
    public void generatedAmountOnlyWaterNameIsTransient() {
        NBTTagCompound tag = named("7950L Water");
        KnownTransientItemStatePolicy.normalize("minecraft:water", 0, tag);
        assertTrue(tag.func_150296_c().isEmpty());
    }

    @Test
    public void arbitraryPlayerRenameIsPreserved() {
        NBTTagCompound tag = named("Cirno Water");
        KnownTransientItemStatePolicy.normalize("minecraft:water", 0, tag);
        assertEquals("Cirno Water", tag.getCompoundTag("display").getString("Name"));
    }

    @Test
    public void amountLookingNameWithLoreIsPreserved() {
        NBTTagCompound tag = named("7868L Water");
        NBTTagCompound display = tag.getCompoundTag("display");
        display.setString("LoreSentinel", "keep me");
        KnownTransientItemStatePolicy.normalize("minecraft:water", 0, tag);
        assertFalse(tag.func_150296_c().isEmpty());
        assertTrue(tag.getCompoundTag("display").hasKey("LoreSentinel"));
    }

    private static NBTTagCompound named(String name) {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", name);
        root.setTag("display", display);
        return root;
    }
}
