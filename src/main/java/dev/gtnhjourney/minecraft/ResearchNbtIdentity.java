package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Semantic NBT identity with narrowly verified handling for supported transient/runtime state. */
public final class ResearchNbtIdentity {

    private ResearchNbtIdentity() {}

    /** Stack-aware identity. Only verified/structurally proven transient payload may be ignored. */
    public static String canonicalize(ItemStack stack) {
        return canonicalize(stack, null);
    }

    /** Stack-aware identity with an already-resolved canonical registry id. */
    public static String canonicalize(ItemStack stack, String canonicalItemId) {
        if (stack == null || !stack.hasTagCompound()) return "";
        NBTTagCompound identityTag = (NBTTagCompound) stack.getTagCompound().copy();
        BotaniaTransientStatePolicy.normalize(stack, identityTag);
        DraconicTransientStatePolicy.normalize(stack, identityTag);
        WearableTransientStatePolicy.normalize(stack, identityTag);
        ThaumcraftWandStatePolicy.normalize(stack, identityTag);
        TransientToolFluidPolicy.normalize(stack, identityTag);
        KnownTransientItemStatePolicy.normalize(stack, identityTag);
        EmbeddedInventoryPolicy.normalize(stack, identityTag);
        ForestryGeneticsNbtPolicy.normalize(stack, identityTag);
        if (canonicalItemId != null) {
            identityTag = CropsNhSeedStatePolicy.identityTag(canonicalItemId, identityTag);
            GalacticraftRocketFuelStatePolicy.normalize(canonicalItemId, identityTag);
            MobSpawnerStatePolicy.normalizeIdentity(canonicalItemId, identityTag);
        }
        final boolean normalizeToolState = ResearchCompatibilityOptions.normalizeGtTransientIdentity()
            && GtToolStatePolicy.isVerifiedTool(stack);
        final boolean normalizeTconWear = ResearchCompatibilityOptions.normalizeTconToolWear()
            && TconToolStatePolicy.isVerifiedTool(stack);
        if (normalizeTconWear) {
            TconToolStatePolicy.normalizeAmmoState(identityTag);
            TconToolStatePolicy.normalizeDisplayName(stack, identityTag);
        }
        if (identityTag == null || identityTag.func_150296_c().isEmpty()) return "";
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
                        || TconToolStatePolicy.isTransientIguanaCounterKey(key)))
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
