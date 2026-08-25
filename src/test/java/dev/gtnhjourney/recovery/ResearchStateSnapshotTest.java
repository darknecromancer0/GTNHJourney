package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Arrays;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class ResearchStateSnapshotTest {

    @Test
    public void statePreservesEntryOrderAndCopiesInput() {
        NBTTagCompound aTag = new NBTTagCompound();
        aTag.setString("FluidName", "water");
        NBTTagCompound bTag = new NBTTagCompound();
        bTag.setLong("charge", 100L);

        ResearchEntrySnapshot a = new ResearchEntrySnapshot(new ResearchKey("IC2:itemFluidCell", 0, "a"), aTag, 0);
        ResearchEntrySnapshot b = new ResearchEntrySnapshot(new ResearchKey("IC2:itemToolDrill", 26, "b"), bTag, 1);
        List<ResearchEntrySnapshot> source = Arrays.asList(a, b);

        ResearchStateSnapshot state = new ResearchStateSnapshot(source);

        assertEquals(2, state.size());
        assertEquals(a.key(), state.entries().get(0).key());
        assertEquals(b.key(), state.entries().get(1).key());
        assertNotSame(source, state.entries());
    }
}
