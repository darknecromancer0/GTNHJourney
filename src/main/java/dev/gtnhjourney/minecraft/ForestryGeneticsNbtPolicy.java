package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Normalizes only storage-width differences in verified Forestry genetic item NBT. */
public final class ForestryGeneticsNbtPolicy {

    private ForestryGeneticsNbtPolicy() {}

    public static void normalize(ItemStack stack, NBTTagCompound tag) {
        if (stack == null || stack.getItem() == null || tag == null || !hasGenome(tag)) return;
        UniqueIdentifier id;
        try {
            id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        } catch (RuntimeException unsafeRegistry) {
            return;
        }
        if (id == null || id.modId == null || !"Forestry".equalsIgnoreCase(id.modId)) return;
        normalizeGeneticsTag(tag);
    }

    static void normalizeGeneticsTag(NBTTagCompound tag) {
        if (tag == null || !hasGenome(tag)) return;
        if (tag.hasKey("IsAnalyzed")) tag.setBoolean("IsAnalyzed", tag.getBoolean("IsAnalyzed"));
        normalizeGenome(tag.getCompoundTag("Genome"));
        if (tag.hasKey("Mate", 10)) normalizeGenome(tag.getCompoundTag("Mate"));
    }

    private static boolean hasGenome(NBTTagCompound tag) {
        return tag != null && tag.hasKey("Genome", 10) && tag.getCompoundTag("Genome").hasKey("Chromosomes", 9);
    }

    private static void normalizeGenome(NBTTagCompound genome) {
        if (genome == null || !genome.hasKey("Chromosomes", 9)) return;
        NBTTagList chromosomes = genome.getTagList("Chromosomes", 10);
        for (int i = 0; i < chromosomes.tagCount(); i++) {
            NBTTagCompound chromosome = chromosomes.getCompoundTagAt(i);
            if (chromosome.hasKey("Slot")) chromosome.setByte("Slot", chromosome.getByte("Slot"));
        }
    }
}
