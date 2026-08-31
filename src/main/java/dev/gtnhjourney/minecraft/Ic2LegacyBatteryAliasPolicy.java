package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Maps IC2's non-electric discharged RE-Battery placeholder to the real rechargeable battery item. */
public final class Ic2LegacyBatteryAliasPolicy {

    static final String DISCHARGED_RE_BATTERY = "IC2:itemBatREDischarged";
    static final String RE_BATTERY = "IC2:itemBatRE";

    private Ic2LegacyBatteryAliasPolicy() {}

    public static String canonicalItemId(String itemId) {
        return DISCHARGED_RE_BATTERY.equals(itemId) ? RE_BATTERY : itemId;
    }

    /**
     * Replaces only the known IC2 RE-Battery placeholder. Unknown items and missing optional IC2 runtime state are
     * returned unchanged so compatibility stays fail-closed.
     */
    public static ItemStack identityStack(ItemStack observed) {
        if (observed == null) return null;
        ItemStack fallback = observed.copy();
        if (observed.getItem() == null) return fallback;
        try {
            UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(observed.getItem());
            if (id == null || !DISCHARGED_RE_BATTERY.equals(id.modId + ":" + id.name)) return fallback;

            Item rechargeable = GameRegistry.findItem("IC2", "itemBatRE");
            if (rechargeable == null) return fallback;
            ItemStack replacement = new ItemStack(rechargeable, observed.stackSize, 0);
            if (observed.hasTagCompound()) {
                replacement.setTagCompound((NBTTagCompound) observed.getTagCompound().copy());
            }
            return replacement;
        } catch (RuntimeException ignored) {
            return fallback;
        } catch (LinkageError ignored) {
            return fallback;
        }
    }
}
