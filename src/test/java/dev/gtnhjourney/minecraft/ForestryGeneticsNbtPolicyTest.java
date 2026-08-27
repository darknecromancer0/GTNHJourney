package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class ForestryGeneticsNbtPolicyTest {

    @Test
    public void byteAndLongEncodedRuntimeNumbersShareResearchIdentity() {
        Item item = new Item();
        ItemStack byteEncoded = geneticsStack(item, false, false, "forestry.treeOak", 0);
        ItemStack longEncoded = geneticsStack(item, true, true, "forestry.treeOak", 0);

        assertEquals(
            ResearchNbtIdentity.canonicalize(byteEncoded),
            ResearchNbtIdentity.canonicalize(longEncoded));
    }

    @Test
    public void alleleUidStillDefinesDifferentGeneticIdentity() {
        Item item = new Item();
        ItemStack oak = geneticsStack(item, false, false, "forestry.treeOak", 0);
        ItemStack birch = geneticsStack(item, true, true, "forestry.treeBirch", 0);

        assertNotEquals(ResearchNbtIdentity.canonicalize(oak), ResearchNbtIdentity.canonicalize(birch));
    }

    @Test
    public void generationRemainsMeaningful() {
        Item item = new Item();
        ItemStack first = geneticsStack(item, false, false, "forestry.treeOak", 1);
        ItemStack later = geneticsStack(item, false, false, "forestry.treeOak", 3);

        assertNotEquals(ResearchNbtIdentity.canonicalize(first), ResearchNbtIdentity.canonicalize(later));
    }

    private static ItemStack geneticsStack(Item item, boolean longSlot, boolean longAnalyzed, String uid, int generation) {
        ItemStack stack = new ItemStack(item, 1, 0);
        NBTTagCompound root = new NBTTagCompound();
        if (longAnalyzed) root.setLong("IsAnalyzed", 0L);
        else root.setBoolean("IsAnalyzed", false);
        if (generation > 0) root.setInteger("GEN", generation);

        NBTTagCompound chromosome = new NBTTagCompound();
        if (longSlot) chromosome.setLong("Slot", 0L);
        else chromosome.setByte("Slot", (byte) 0);
        chromosome.setString("UID0", uid);
        chromosome.setString("UID1", uid);

        NBTTagList chromosomes = new NBTTagList();
        chromosomes.appendTag(chromosome);
        NBTTagCompound genome = new NBTTagCompound();
        genome.setTag("Chromosomes", chromosomes);
        root.setTag("Genome", genome);
        stack.setTagCompound(root);
        return stack;
    }
}
