package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.registry.GameRegistry;

/** Registry-scoped cleanup for wearable runtime fields verified from live GTNH data. */
public final class WearableTransientStatePolicy {

    private static final String EMT_QUANTUM_CHEST = "EMT:itemArmorQuantumChestplate";
    private static final String WYVERN_CHEST = "DraconicEvolution:wyvernChest";

    private WearableTransientStatePolicy() {}

    public static void normalize(ItemStack owner, NBTTagCompound tag) {
        if (tag == null) return;
        String registryId = registryId(owner);
        if (registryId != null) normalize(registryId, tag);
    }

    static void normalize(String registryId, NBTTagCompound tag) {
        if (registryId == null || tag == null) return;
        if (EMT_QUANTUM_CHEST.equals(registryId)) {
            removeZeroNumeric(tag, "unequip");
            removeZeroNumeric(tag, "wing");
            return;
        }
        if (WYVERN_CHEST.equals(registryId)) {
            tag.removeTag("ProtectionPoints");
            tag.removeTag("ShieldEntropy");
        }
    }

    public static boolean matches(ItemStack stack) {
        String registryId = registryId(stack);
        return EMT_QUANTUM_CHEST.equals(registryId) || WYVERN_CHEST.equals(registryId);
    }

    static String registryId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static void removeZeroNumeric(NBTTagCompound tag, String key) {
        if (tag.hasKey(key, 99) && tag.getDouble(key) == 0.0D) tag.removeTag(key);
    }
}
