package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Semantic handling for legacy Extra Utilities and current UtilitiesInExcess ticking unstable-ingot runtime state. */
public final class UnstableIngotStatePolicy {

    static final String LEGACY_UNSTABLE_INGOT = "ExtraUtilities:unstableingot";
    static final String UIE_INVERTED_INGOT = "utilitiesinexcess:inverted_ingot";

    private static final String LEGACY_TIME = "time";
    private static final String LEGACY_DIMENSION = "dimension";
    private static final String LEGACY_CREATIVE = "creative";

    private static final String UIE_CRAFTED = "Crafted";
    private static final String UIE_CRAFTED_AT = "CraftedAt";

    private UnstableIngotStatePolicy() {}

    public static void normalizeIdentity(ItemStack stack, NBTTagCompound tag) {
        if (stack == null || stack.getItem() == null || tag == null) return;
        String itemId = registryId(stack);
        if (itemId == null) return;
        normalizeIdentity(itemId, stack.getItemDamage(), tag);
    }

    static void normalizeIdentity(String itemId, int meta, NBTTagCompound tag) {
        if (tag == null || meta != 0) return;

        if (LEGACY_UNSTABLE_INGOT.equals(itemId)) {
            tag.removeTag(LEGACY_TIME);
            tag.removeTag(LEGACY_DIMENSION);
            return;
        }

        if (UIE_INVERTED_INGOT.equals(itemId)) {
            // GTNH's current unstable ingot recipe emits {Crafted:true}. UtilitiesInExcess adds CraftedAt later as
            // a per-instance world timestamp. CraftedAt is runtime state and must never become Journey identity.
            // If an old template somehow retained CraftedAt without Crafted, force the semantic crafted flag so
            // removing the expired timestamp cannot accidentally turn the dangerous ingot into an untagged safe one.
            if (tag.hasKey(UIE_CRAFTED_AT, 99)) tag.setBoolean(UIE_CRAFTED, true);
            tag.removeTag(UIE_CRAFTED_AT);
        }
    }

    /**
     * Journey stores one semantic unstable-ingot entry regardless of the ticking timestamp. Legacy Extra Utilities
     * needs a fresh explicit time/dimension pair; current UtilitiesInExcess wants the native recipe form
     * {Crafted:true} with no CraftedAt so its own first tick initializes the countdown.
     */
    public static ItemStack refreshForRetrieval(ItemStack stack, EntityPlayerMP player) {
        if (stack == null || player == null || player.worldObj == null) return stack;
        String itemId = registryId(stack);
        if (itemId == null) return stack;
        return refreshForRetrieval(
            stack,
            itemId,
            stack.getItemDamage(),
            player.worldObj.getTotalWorldTime(),
            player.dimension);
    }

    static ItemStack refreshForRetrieval(ItemStack stack, String itemId, int meta, long worldTime, int dimension) {
        if (stack == null || meta != 0) return stack;

        if (LEGACY_UNSTABLE_INGOT.equals(itemId)) {
            NBTTagCompound tag = stack.hasTagCompound()
                ? (NBTTagCompound) stack.getTagCompound().copy()
                : new NBTTagCompound();

            // Legacy creative unstable ingots are intentionally non-expiring.
            if (tag.hasKey(LEGACY_CREATIVE, 1) && tag.getBoolean(LEGACY_CREATIVE)) return stack;

            tag.setLong(LEGACY_TIME, worldTime);
            tag.setInteger(LEGACY_DIMENSION, dimension);
            stack.setTagCompound(tag);
            return stack;
        }

        if (UIE_INVERTED_INGOT.equals(itemId) && stack.hasTagCompound()) {
            NBTTagCompound tag = (NBTTagCompound) stack.getTagCompound().copy();
            if (tag.hasKey(UIE_CRAFTED, 1) && tag.getBoolean(UIE_CRAFTED)) {
                tag.removeTag(UIE_CRAFTED_AT);
                stack.setTagCompound(tag);
            }
        }
        return stack;
    }

    static boolean matches(String itemId, int meta) {
        return meta == 0 && (LEGACY_UNSTABLE_INGOT.equals(itemId) || UIE_INVERTED_INGOT.equals(itemId));
    }

    private static String registryId(ItemStack stack) {
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
