package dev.gtnhjourney.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Preserves IC2's native split RE-Battery representation: the crafted empty battery is itemBatREDischarged while
 * charged states use itemBatRE.
 */
public final class Ic2LegacyBatteryAliasPolicy {

    static final String DISCHARGED_RE_BATTERY = "IC2:itemBatREDischarged";
    static final String RE_BATTERY = "IC2:itemBatRE";
    private static final String CHARGE_KEY = "charge";

    private Ic2LegacyBatteryAliasPolicy() {}

    /** Kept for compatibility with older callers; native registry identity is now always preserved. */
    public static String canonicalItemId(String itemId) {
        return itemId;
    }

    /**
     * Canonicalizes only the empty endpoint of the rechargeable IC2 item back to the actual crafted discharged item.
     * Positive-charge states remain itemBatRE so the generic IC2 endpoint policy can normalize them normally.
     */
    public static ItemStack identityStack(ItemStack observed) {
        if (observed == null) return null;
        ItemStack fallback = observed.copy();
        if (observed.getItem() == null) return fallback;

        String itemId = registryId(observed);
        if (DISCHARGED_RE_BATTERY.equals(itemId)) return fallback;
        if (!RE_BATTERY.equals(itemId)) return fallback;
        if (Ic2ChargeStatePolicy.classify(observed) != Ic2ChargeStatePolicy.State.BASE) return fallback;

        ItemStack discharged = toDischarged(observed);
        return discharged == null ? fallback : discharged;
    }

    /**
     * Expands RE-Battery observations while preserving IC2's cross-item endpoint semantics. A crafted discharged
     * battery unlocks only the crafted empty endpoint. A charged battery unlocks that same empty endpoint plus the
     * normalized charged endpoint.
     */
    public static List<ItemStack> expand(ItemStack observed) {
        if (observed == null || observed.getItem() == null) return Collections.emptyList();
        String itemId = registryId(observed);

        if (DISCHARGED_RE_BATTERY.equals(itemId)) {
            ItemStack base = observed.copy();
            base.stackSize = 1;
            return Collections.singletonList(base);
        }
        if (!RE_BATTERY.equals(itemId)) return Collections.emptyList();

        Ic2ChargeStatePolicy.State state = Ic2ChargeStatePolicy.classify(observed);
        if (state == Ic2ChargeStatePolicy.State.EXACT) return Collections.emptyList();
        if (state == Ic2ChargeStatePolicy.State.BASE) {
            ItemStack base = toDischarged(observed);
            if (base == null) {
                ItemStack exact = observed.copy();
                exact.stackSize = 1;
                return Collections.singletonList(exact);
            }
            return Collections.singletonList(base);
        }

        List<ItemStack> genericEndpoints = Ic2ChargeStatePolicy.expand(observed);
        if (genericEndpoints.isEmpty()) return Collections.emptyList();
        List<ItemStack> endpoints = new ArrayList<ItemStack>(genericEndpoints.size());
        for (ItemStack endpoint : genericEndpoints) {
            if (endpoint == null || endpoint.getItem() == null) continue;
            if (Ic2ChargeStatePolicy.classify(endpoint) == Ic2ChargeStatePolicy.State.BASE) {
                ItemStack base = toDischarged(endpoint);
                if (base != null) {
                    endpoints.add(base);
                    continue;
                }
            }
            ItemStack copy = endpoint.copy();
            copy.stackSize = 1;
            endpoints.add(copy);
        }
        return endpoints.isEmpty() ? Collections.<ItemStack>emptyList() : Collections.unmodifiableList(endpoints);
    }

    /**
     * Migrates the bad 1.1.36 empty representation (itemBatRE with discharged damage and no charge NBT) back to the
     * native crafted registry item. Charged itemBatRE states are preserved.
     */
    static String migratePersistedItemId(String itemId, int meta, NBTTagCompound template) {
        return isLegacyAliasedBase(itemId, meta, template) ? DISCHARGED_RE_BATTERY : itemId;
    }

    static int migratePersistedMeta(String itemId, int meta, NBTTagCompound template) {
        return isLegacyAliasedBase(itemId, meta, template) ? 0 : meta;
    }

    private static boolean isLegacyAliasedBase(String itemId, int meta, NBTTagCompound template) {
        return RE_BATTERY.equals(itemId) && meta > 0 && (template == null || !template.hasKey(CHARGE_KEY));
    }

    private static ItemStack toDischarged(ItemStack observed) {
        try {
            Item discharged = GameRegistry.findItem("IC2", "itemBatREDischarged");
            if (discharged == null) return null;

            ItemStack replacement = new ItemStack(discharged, 1, 0);
            if (observed != null && observed.hasTagCompound()) {
                NBTTagCompound tag = (NBTTagCompound) observed.getTagCompound().copy();
                tag.removeTag(CHARGE_KEY);
                if (!tag.func_150296_c().isEmpty()) replacement.setTagCompound(tag);
            }
            return replacement;
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static String registryId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
