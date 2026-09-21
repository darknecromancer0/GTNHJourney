package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Vanilla equipment enchantments are mutable upgrades, not separate Journey discoveries. Custom display/lore NBT is
 * intentionally preserved so named/story items remain distinct while ordinary enchanted swords/armor collapse to base.
 */
public final class VanillaEquipmentStatePolicy {

    private VanillaEquipmentStatePolicy() {}

    public static void normalize(ItemStack stack, NBTTagCompound tag) {
        if (stack == null || stack.getItem() == null || tag == null) return;
        String itemId = registryId(stack);
        normalize(itemId, tag);
    }

    static void normalize(String itemId, NBTTagCompound tag) {
        if (tag == null || !isVanillaEquipment(itemId)) return;
        tag.removeTag("ench");
        tag.removeTag("RepairCost");
    }

    static boolean isVanillaEquipment(String itemId) {
        if (itemId == null || !itemId.startsWith("minecraft:")) return false;
        String name = itemId.substring("minecraft:".length());
        if ("bow".equals(name)) return true;
        return name.endsWith("_sword")
            || name.endsWith("_pickaxe")
            || name.endsWith("_axe")
            || name.endsWith("_shovel")
            || name.endsWith("_hoe")
            || name.endsWith("_helmet")
            || name.endsWith("_chestplate")
            || name.endsWith("_leggings")
            || name.endsWith("_boots");
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
