package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Semantic handling for Extra Utilities' ticking unstable ingot runtime state. */
public final class UnstableIngotStatePolicy {

    static final String UNSTABLE_INGOT = "ExtraUtilities:unstableingot";
    private static final String TIME = "time";
    private static final String DIMENSION = "dimension";
    private static final String CREATIVE = "creative";

    private UnstableIngotStatePolicy() {}

    public static void normalizeIdentity(ItemStack stack, NBTTagCompound tag) {
        if (stack == null || stack.getItem() == null || tag == null) return;
        String itemId = registryId(stack);
        if (itemId == null) return;
        normalizeIdentity(itemId, stack.getItemDamage(), tag);
    }

    static void normalizeIdentity(String itemId, int meta, NBTTagCompound tag) {
        if (!matches(itemId, meta) || tag == null) return;
        tag.removeTag(TIME);
        tag.removeTag(DIMENSION);
    }

    /**
     * Journey stores one semantic unstable-ingot entry regardless of the ticking timestamp. Physical retrieval must
     * restore a fresh timer, otherwise replaying the old persisted timestamp would produce an already-expired ingot.
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
        if (stack == null || !matches(itemId, meta)) return stack;

        NBTTagCompound tag = stack.hasTagCompound()
            ? (NBTTagCompound) stack.getTagCompound().copy()
            : new NBTTagCompound();

        // Creative unstable ingots are intentionally non-expiring and must stay that way.
        if (tag.hasKey(CREATIVE, 1) && tag.getBoolean(CREATIVE)) return stack;

        tag.setLong(TIME, worldTime);
        tag.setInteger(DIMENSION, dimension);
        stack.setTagCompound(tag);
        return stack;
    }

    static boolean matches(String itemId, int meta) {
        return UNSTABLE_INGOT.equals(itemId) && meta == 0;
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
