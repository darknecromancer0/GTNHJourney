package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Semantic NBT identity with narrowly verified handling for GT generated-tool runtime state. */
public final class ResearchNbtIdentity {

    private ResearchNbtIdentity() {}

    /** Stack-aware identity. Only verified GT generated tools may ignore Damage/Mode under GT.ToolStats. */
    public static String canonicalize(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return "";
        NBTTagCompound identityTag = (NBTTagCompound) stack.getTagCompound()
            .copy();
        BotaniaTransientStatePolicy.normalize(stack, identityTag);
        DraconicTransientStatePolicy.normalize(stack, identityTag);
        if (identityTag.func_150296_c()
            .isEmpty()) return "";
        final boolean normalizeToolState = ResearchCompatibilityOptions.normalizeGtTransientIdentity()
            && GtToolStatePolicy.isVerifiedTool(stack);
        final boolean normalizeTconWear = ResearchCompatibilityOptions.normalizeTconToolWear()
            && TconToolStatePolicy.isVerifiedTool(stack);
        return NbtCanonicalizer.canonicalize(identityTag, new NbtCanonicalizer.KeyFilter() {

            @Override
            public boolean include(String parentPath, String key) {
                if (normalizeToolState && "GT.ToolStats".equals(parentPath)
                    && ("Damage".equals(key) || "Mode".equals(key))) return false;
                if (normalizeTconWear && "InfiTool".equals(parentPath)
                    && ("Damage".equals(key) || "Broken".equals(key)
                        || "RenderBroken".equals(key)
                        || "ToolEXP".equals(key)
                        || "HeadEXP".equals(key)
                        || "ExtraRedstone".equals(key)))
                    return false;
                return true;
            }
        });
    }

    /** Tag-only fallback is deliberately exact because the owning item class is unknown. */
    public static String canonicalize(NBTTagCompound tag) {
        return NbtCanonicalizer.canonicalize(tag);
    }
}
