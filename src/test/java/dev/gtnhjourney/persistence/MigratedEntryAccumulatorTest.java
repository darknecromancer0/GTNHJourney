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
    public void firstValidOccurrenceWinsCollapsedIdentityAndChronology() {
        MigratedEntryAccumulator accumulator = new MigratedEntryAccumulator();
        ResearchKey collapsed = new ResearchKey("EMT:itemArmorQuantumChestplate", 0, "persistent");
        ResearchKey distinct = new ResearchKey("IC2:itemFluidCell", 0, "fluid=molten.orundum");
        NBTTagCompound earliest = tag("source", "earliest");
        NBTTagCompound later = tag("source", "later");
        NBTTagCompound fluid = tag("FluidName", "molten.orundum");

        assertTrue(accumulator.accept(collapsed, earliest));
        assertFalse(accumulator.accept(collapsed, later));
        assertTrue(accumulator.accept(distinct, fluid));

        assertEquals(Arrays.asList(collapsed, distinct), accumulator.keys());
        assertEquals("earliest", accumulator.template(collapsed).getString("source"));
        assertEquals("molten.orundum", accumulator.template(distinct).getString("FluidName"));
    }

    @Test
    public void templatesAreDefensivelyCopiedAndRejectedEntriesCannotReplaceTheSurvivor() {
        MigratedEntryAccumulator accumulator = new MigratedEntryAccumulator();
        ResearchKey key = new ResearchKey("test:item", 0, "state");
        NBTTagCompound earliest = tag("value", "A");

        assertTrue(accumulator.accept(key, earliest));
        earliest.setString("value", "mutated-after-accept");
        NBTTagCompound returned = accumulator.template(key);
        returned.setString("value", "mutated-output");

        assertEquals("A", accumulator.template(key).getString("value"));
        assertFalse(accumulator.accept(key, tag("value", "B")));
        assertEquals("A", accumulator.template(key).getString("value"));
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
