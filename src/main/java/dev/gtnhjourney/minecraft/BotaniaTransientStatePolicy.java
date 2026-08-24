package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Removes the verified per-tick cooldown timer from Botania magnet-ring research identity/templates. */
public final class BotaniaTransientStatePolicy {

    private static final String MAGNET_RING_CLASS = "vazkii.botania.common.item.equipment.bauble.ItemMagnetRing";
    private static final String COOLDOWN_KEY = "cooldown";
    private static final Class<?> MAGNET_RING = load(MAGNET_RING_CLASS);

    private BotaniaTransientStatePolicy() {}

    public static boolean isVerifiedMagnetRing(ItemStack stack) {
        return stack != null && stack.getItem() != null
            && MAGNET_RING != null
            && MAGNET_RING.isInstance(stack.getItem());
    }

    public static boolean isRuntimeAvailable() {
        return MAGNET_RING != null;
    }

    public static void normalize(ItemStack owner, NBTTagCompound tag) {
        if (tag == null || !isVerifiedMagnetRing(owner)) return;
        tag.removeTag(COOLDOWN_KEY);
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, BotaniaTransientStatePolicy.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
