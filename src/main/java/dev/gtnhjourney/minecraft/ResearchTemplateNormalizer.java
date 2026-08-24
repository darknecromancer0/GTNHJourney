package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/** Normalizes only verified transient GT generated-tool state in the template Journey will later recreate. */
public final class ResearchTemplateNormalizer {
    private ResearchTemplateNormalizer() {}

    public static NBTTagCompound normalize(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        NBTTagCompound out = (NBTTagCompound) stack.getTagCompound().copy();
        if (ResearchCompatibilityOptions.resetGtToolTemplateState() && GtToolStatePolicy.isVerifiedTool(stack)) {
            NBTBase rawToolStats = out.getTag("GT.ToolStats");
            if (rawToolStats instanceof NBTTagCompound) {
                NBTTagCompound toolStats = (NBTTagCompound) rawToolStats;
                if (toolStats.hasKey("Damage")) toolStats.setLong("Damage", 0L);
                if (toolStats.hasKey("Mode")) toolStats.setByte("Mode", (byte) 0);
            }
        }
        if (ResearchCompatibilityOptions.normalizeTconToolWear() && TconToolStatePolicy.isVerifiedTool(stack)) {
            NBTBase rawInfiTool = out.getTag("InfiTool");
            if (rawInfiTool instanceof NBTTagCompound) {
                NBTTagCompound infiTool = (NBTTagCompound) rawInfiTool;
                if (infiTool.hasKey("Damage")) infiTool.setInteger("Damage", 0);
                if (infiTool.hasKey("Broken")) infiTool.setBoolean("Broken", false);
            }
        }
        return out;
    }

    /** Tag-only fallback is deliberately exact because the owning item class is unknown. */
    public static NBTTagCompound normalize(NBTTagCompound original) {
        return original == null ? null : (NBTTagCompound) original.copy();
    }
}
