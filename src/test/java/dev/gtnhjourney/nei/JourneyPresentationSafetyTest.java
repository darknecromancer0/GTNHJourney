package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class JourneyPresentationSafetyTest {

    @Test
    public void sanitizedFlaskDisplayTagNeverMutatesAuthoritativeTemplate() {
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", "fuelgc");
        fluid.setInteger("Amount", 32000);
        NBTTagCompound authoritative = new NBTTagCompound();
        authoritative.setInteger("Capacity", 32000);
        authoritative.setTag("Fluid", fluid);

        NBTTagCompound display = JourneyPresentationSafety.sanitizedFlaskTag(authoritative);

        assertTrue(authoritative.hasKey("Fluid"));
        assertFalse(display.hasKey("Fluid"));
        assertEquals(32000, display.getInteger("Capacity"));
    }

    @Test
    public void catastrophicThirdPartyPresentationFailureIsOmittedAndCounted() {
        JourneyRuntimeCounters.reset();

        assertNull(JourneyPanelController.safePresentation(new ExplodingItemStack()));
        assertEquals(1L, JourneyRuntimeCounters.snapshot().getPresentationFailures());
    }

    private static final class ExplodingItemStack extends ItemStack {

        private ExplodingItemStack() {
            super(new Item(), 1, 0);
        }

        @Override
        public ItemStack copy() {
            throw new AssertionError("simulated third-party renderer state failure");
        }
    }
}
