package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;

public class MigratedEntryAccumulatorTest {

    @Test
    public void newestEquivalentOccurrenceWinsCollapsedIdentityAndChronology() {
        MigratedEntryAccumulator accumulator = new MigratedEntryAccumulator();
        ResearchKey collapsed = new ResearchKey("Botania:specialFlower", 0, "type=daybloom");
        ResearchKey distinct = new ResearchKey("IC2:itemFluidCell", 0, "fluid=molten.orundum");
        NBTTagCompound earliest = tag("source", "earliest");
        NBTTagCompound fluid = tag("FluidName", "molten.orundum");
        NBTTagCompound later = tag("source", "later");

        assertTrue(accumulator.accept(collapsed, earliest));
        assertTrue(accumulator.accept(distinct, fluid));
        assertFalse(accumulator.accept(collapsed, later));

        assertEquals(Arrays.asList(distinct, collapsed), accumulator.keys());
        assertEquals("later", accumulator.template(collapsed).getString("source"));
        assertEquals("molten.orundum", accumulator.template(distinct).getString("FluidName"));
    }

    @Test
    public void templatesAreDefensivelyCopiedAndNewestEquivalentReplacesTheSurvivor() {
        MigratedEntryAccumulator accumulator = new MigratedEntryAccumulator();
        ResearchKey key = new ResearchKey("test:item", 0, "state");
        NBTTagCompound earliest = tag("value", "A");

        assertTrue(accumulator.accept(key, earliest));
        earliest.setString("value", "mutated-after-accept");
        NBTTagCompound returned = accumulator.template(key);
        returned.setString("value", "mutated-output");

        assertEquals("A", accumulator.template(key).getString("value"));
        assertFalse(accumulator.accept(key, tag("value", "B")));
        assertEquals("B", accumulator.template(key).getString("value"));
    }

    @Test
    public void nullKeyIsRejectedWithoutReservingChronology() {
        MigratedEntryAccumulator accumulator = new MigratedEntryAccumulator();

        assertFalse(accumulator.accept(null, tag("value", "invalid")));
        assertTrue(accumulator.keys().isEmpty());
        assertNull(accumulator.template(null));
    }

    private static NBTTagCompound tag(String key, String value) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(key, value);
        return tag;
    }
}
