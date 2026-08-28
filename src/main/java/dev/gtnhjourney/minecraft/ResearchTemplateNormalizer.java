package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/** Normalizes only verified transient/runtime state in the template Journey will later recreate. */
public final class ResearchTemplateNormalizer {

    private ResearchTemplateNormalizer() {}

    public static NBTTagCompound normalize(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        NBTTagCompound out = (NBTTagCompound) stack.getTagCompound()
            .copy();
        BotaniaTransientStatePolicy.normalize(stack, out);
        DraconicTransientStatePolicy.normalize(stack, out);
        WearableTransientStatePolicy.normalize(stack, out);
        TransientToolFluidPolicy.normalize(stack, out);
        KnownTransientItemStatePolicy.normalize(stack, out);
        EmbeddedInventoryPolicy.normalize(stack, out);
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
                out.setTag("InfiTool", normalizeTconWearState((NBTTagCompound) rawInfiTool));
                TconToolStatePolicy.normalizeAmmoState(out);
            }
        }
        return out.func_150296_c()
            .isEmpty() ? null : out;
    }

    /**
     * Resets only TCon durability/render wear. Level, XP and modifiers are real tool state and must survive retrieval.
     * Legacy pre5 templates that already lost XP counters are repaired to the safest valid zero-progress state.
     */
    static NBTTagCompound normalizeTconWearState(NBTTagCompound original) {
        NBTTagCompound infiTool = original == null ? new NBTTagCompound() : (NBTTagCompound) original.copy();
        if (infiTool.hasKey("Damage")) infiTool.setInteger("Damage", 0);
        if (infiTool.hasKey("Broken")) infiTool.setBoolean("Broken", false);
        infiTool.removeTag("RenderBroken");
        if (infiTool.hasKey("ToolLevel") && infiTool.getInteger("ToolLevel") > 0 && !infiTool.hasKey("ToolEXP")) {
            infiTool.setLong("ToolEXP", 0L);
        }
        if (infiTool.hasKey("HarvestLevelModified") && !infiTool.hasKey("HeadEXP")) {
            infiTool.setLong("HeadEXP", 0L);
        }
        return infiTool;
    }

    /** Tag-only fallback is deliberately exact because the owning item class is unknown. */
    public static NBTTagCompound normalize(NBTTagCompound original) {
        return original == null ? null : (NBTTagCompound) original.copy();
    }
}
