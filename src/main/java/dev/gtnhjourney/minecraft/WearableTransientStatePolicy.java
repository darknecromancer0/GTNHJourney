package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.registry.GameRegistry;

/** Registry-scoped cleanup for wearable runtime fields verified from live GTNH data. */
public final class WearableTransientStatePolicy {

    private static final String EMT_QUANTUM_CHEST = "EMT:itemArmorQuantumChestplate";
    private static final String WYVERN_CHEST = "DraconicEvolution:wyvernChest";
    private static final String ADVENTURE_BACKPACK_COAL_JETPACK = "adventurebackpack:coalJetpack";

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
            return;
        }
        if (ADVENTURE_BACKPACK_COAL_JETPACK.equals(registryId)) {
            // Live GTNH dumps show this compound continuously changing with heat, burn/cool counters, water/steam tank
            // contents and leak/boil state. Those values describe the currently running wearable, not a distinct item
            // identity. Journey retrieves the clean/base jetpack and lets Adventure Backpack rebuild operational state.
            tag.removeTag("wearableData");
        }
    }

    public static boolean matches(ItemStack stack) {
        String registryId = registryId(stack);
        return EMT_QUANTUM_CHEST.equals(registryId)
            || WYVERN_CHEST.equals(registryId)
            || ADVENTURE_BACKPACK_COAL_JETPACK.equals(registryId);
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
