package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class RailcraftTankStructureStatePolicyTest {

    @Test
    public void defaultWhiteColorDoesNotSplitRailcraftTankStructureResearch() {
        for (int meta : new int[] { 0, 1, 2, 13, 14, 15 }) {
            NBTTagCompound tag = taggedDefaultWhite();
            KnownTransientItemStatePolicy.normalize("Railcraft:machine.beta", meta, tag);
            assertFalse(tag.hasKey("color"), "beta meta " + meta);
            assertEquals("keep", tag.getString("Marker"));
        }

        for (int meta : new int[] { 3, 4, 5 }) {
            NBTTagCompound tag = taggedDefaultWhite();
            KnownTransientItemStatePolicy.normalize("Railcraft:machine.zeta", meta, tag);
            assertFalse(tag.hasKey("color"), "zeta meta " + meta);
            assertEquals("keep", tag.getString("Marker"));
        }
    }

    @Test
    public void paintedTankStructureAndUnrelatedMachinesKeepTheirColorState() {
        NBTTagCompound paintedTank = new NBTTagCompound();
        paintedTank.setByte("color", (byte) 4);
        KnownTransientItemStatePolicy.normalize("Railcraft:machine.beta", 0, paintedTank);
        assertTrue(paintedTank.hasKey("color"));
        assertEquals(4, paintedTank.getByte("color"));

        NBTTagCompound unrelatedMachine = taggedDefaultWhite();
        KnownTransientItemStatePolicy.normalize("Railcraft:machine.beta", 3, unrelatedMachine);
        assertTrue(unrelatedMachine.hasKey("color"));
        assertEquals(15, unrelatedMachine.getByte("color"));
    }

    private static NBTTagCompound taggedDefaultWhite() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("color", (byte) 15);
        tag.setString("Marker", "keep");
        return tag;
    }
}
