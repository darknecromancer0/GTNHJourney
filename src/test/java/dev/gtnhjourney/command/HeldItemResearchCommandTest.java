package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class HeldItemResearchCommandTest {

    @Test
    public void emptyHandProducesNoResearchCandidate() {
        assertTrue(HeldItemResearchCommand.candidates(null).isEmpty());
    }

    @Test
    public void heldStackIsDefensivelyCopiedWithItsCurrentState() {
        ItemStack held = new ItemStack(Items.stick, 3, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("JourneyTest", "current");
        held.setTagCompound(tag);

        List<ItemStack> candidates = HeldItemResearchCommand.candidates(held);

        assertEquals(1, candidates.size(), "candidate count");
        ItemStack candidate = candidates.get(0);
        assertNotSame(held, candidate, "candidate object identity");
        assertEquals(3, candidate.stackSize, "stack size");
        assertNotNull(candidate.getTagCompound(), "candidate NBT");
        assertEquals("current", candidate.getTagCompound().getString("JourneyTest"), "candidate NBT value");

        candidate.getTagCompound().setString("JourneyTest", "changed-copy");
        assertEquals("current", held.getTagCompound().getString("JourneyTest"), "held NBT must stay independent");
    }
}
