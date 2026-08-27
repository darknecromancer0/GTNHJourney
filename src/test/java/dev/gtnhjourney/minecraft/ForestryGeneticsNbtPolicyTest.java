package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class ForestryGeneticsNbtPolicyTest {

    @Test
    public void byteAndLongEncodedRuntimeNumbersNormalizeIdentically() {
        NBTTagCompound byteEncoded = geneticsTag(false, false, "forestry.treeOak", 0);
        NBTTagCompound longEncoded = geneticsTag(true, true, "forestry.treeOak", 0);

        ForestryGeneticsNbtPolicy.normalizeGeneticsTag(byteEncoded);
        ForestryGeneticsNbtPolicy.normalizeGeneticsTag(longEncoded);

        assertEquals(NbtCanonicalizer.canonicalize(byteEncoded), NbtCanonicalizer.canonicalize(longEncoded));
    }

    @Test
    public void alleleUidStillDefinesDifferentGeneticIdentity() {
        NBTTagCompound oak = geneticsTag(false, false, "forestry.treeOak", 0);
        NBTTagCompound birch = geneticsTag(true, true, "forestry.treeBirch", 0);

        ForestryGeneticsNbtPolicy.normalizeGeneticsTag(oak);
        ForestryGeneticsNbtPolicy.normalizeGeneticsTag(birch);

        assertNotEquals(NbtCanonicalizer.canonicalize(oak), NbtCanonicalizer.canonicalize(birch));
    }

    @Test
    public void generationRemainsMeaningful() {
        NBTTagCompound first = geneticsTag(false, false, "forestry.treeOak", 1);
        NBTTagCompound later = geneticsTag(false, false, "forestry.treeOak", 3);

        ForestryGeneticsNbtPolicy.normalizeGeneticsTag(first);
        ForestryGeneticsNbtPolicy.normalizeGeneticsTag(later);

        assertNotEquals(NbtCanonicalizer.canonicalize(first), NbtCanonicalizer.canonicalize(later));
    }

    private static NBTTagCompound geneticsTag(boolean longSlot, boolean longAnalyzed, String uid, int generation) {
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
        return root;
    }
}
