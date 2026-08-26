package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class FilledContainerIdentityTest {

    @Test
    public void differentFilledFluidPayloadsRemainDistinctAndTemplateNormalizationStaysExact() {
        NBTTagCompound orundum = fluidTag("molten.orundum", 1000);
        NBTTagCompound vanadium = fluidTag("molten.vanadium", 1000);

        assertNotEquals(NbtCanonicalizer.canonicalize(orundum), NbtCanonicalizer.canonicalize(vanadium));
        assertEquals(
            NbtCanonicalizer.canonicalize(orundum),
            NbtCanonicalizer.canonicalize(ResearchTemplateNormalizer.normalize(orundum)));
    }

    @Test
    public void emptyAndFilledContainersRemainDifferentStates() {
        NBTTagCompound empty = new NBTTagCompound();
        empty.setInteger("Capacity", 1000);
        NBTTagCompound filled = (NBTTagCompound) empty.copy();
        filled.setTag("Fluid", fluidPayload("molten.orundum", 1000));

        assertNotEquals(NbtCanonicalizer.canonicalize(empty), NbtCanonicalizer.canonicalize(filled));
    }

    private static NBTTagCompound fluidTag(String name, int amount) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Capacity", amount);
        tag.setTag("Fluid", fluidPayload(name, amount));
        return tag;
    }

    private static NBTTagCompound fluidPayload(String name, int amount) {
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", name);
        fluid.setInteger("Amount", amount);
        return fluid;
    }
}
