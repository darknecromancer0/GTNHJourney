package dev.gtnhjourney.acquisition;

import dev.gtnhjourney.minecraft.BotaniaTransientStatePolicy;
import dev.gtnhjourney.minecraft.CofhChargeStatePolicy;
import dev.gtnhjourney.minecraft.GtChargeStatePolicy;
import dev.gtnhjourney.minecraft.GtToolStatePolicy;
import dev.gtnhjourney.minecraft.Ic2ChargeStatePolicy;
import dev.gtnhjourney.minecraft.OpenComputersChargeStatePolicy;
import dev.gtnhjourney.minecraft.ResearchNbtIdentity;
import dev.gtnhjourney.minecraft.TconToolStatePolicy;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Cheap, non-authoritative slot signature.
 *
 * <p>Runtime wear and partial electric charge deliberately collapse to stable signatures so a charging battery or
 * used tool does not rebuild full Journey identity every scan tick. Stable subtype metadata is still retained so
 * different electric meta-items sharing one Item cannot mask each other until the periodic forced deep scan.</p>
 *
 * <p>The periodic forced deep scan remains the safety net for hash collisions and for meaningful payload mutations
 * that coexist with a BASE charge endpoint.</p>
 */
public final class InventoryStackSignature {
    private InventoryStackSignature() {}

    public static int of(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return 0;
        int hash = 17;
        try { hash = 31 * hash + Item.getIdFromItem(stack.getItem()); }
        catch (RuntimeException ignored) { hash = 31 * hash + System.identityHashCode(stack.getItem()); }
        catch (LinkageError ignored) { hash = 31 * hash + System.identityHashCode(stack.getItem()); }

        int endpoint = chargeEndpoint(stack);
        if (endpoint != 0) return combineEndpointSignature(hash, fastMeta(stack), endpoint);

        hash = 31 * hash + fastMeta(stack);

        try {
            if (GtToolStatePolicy.isVerifiedTool(stack) || TconToolStatePolicy.isVerifiedTool(stack)
                || BotaniaTransientStatePolicy.isVerifiedMagnetRing(stack)) {
                hash = 31 * hash + ResearchNbtIdentity.canonicalize(stack).hashCode();
            } else {
                NBTTagCompound tag = stack.getTagCompound();
                hash = 31 * hash + (tag == null ? 0 : tag.hashCode());
            }
        } catch (IllegalArgumentException ignored) {
            // The forced safety scan will still attempt the authoritative path and log the malformed state.
        } catch (RuntimeException ignored) {}
        catch (LinkageError ignored) {}
        // stackSize is intentionally excluded: amount is not part of Journey research identity.
        return hash;
    }

    /**
     * Metadata that is cheap and stable enough for the scan cache: real subtype/meta IDs survive, classic durability
     * wear collapses to zero. This mirrors Journey's broad metadata rule without doing full NBT identity work.
     */
    static int fastMeta(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        try {
            int meta = stack.getItemDamage();
            if (stack.getItem().isDamageable() && !stack.getItem().getHasSubtypes()) return 0;
            return meta;
        } catch (RuntimeException ignored) {
            return 0;
        } catch (LinkageError ignored) {
            return 0;
        }
    }

    /** Pure helper kept package-visible so regression tests can lock endpoint composition without GT runtime setup. */
    static int combineEndpointSignature(int itemHash, int stableMeta, int endpoint) {
        int hash = 31 * itemHash + stableMeta;
        return 31 * hash + endpoint;
    }

    /** Non-zero means the authoritative policy proved this is a BASE or FULL electric endpoint. */
    private static int chargeEndpoint(ItemStack stack) {
        try {
            String gt = GtChargeStatePolicy.describe(stack);
            if ("BASE".equals(gt)) return 101;
            if ("FULL".equals(gt)) return 102;

            String oc = OpenComputersChargeStatePolicy.describe(stack);
            if ("BASE".equals(oc)) return 301;
            if ("FULL".equals(oc)) return 302;

            String ic2 = Ic2ChargeStatePolicy.describe(stack);
            if ("BASE".equals(ic2)) return 201;
            if ("FULL".equals(ic2)) return 202;

            String cofh = CofhChargeStatePolicy.describe(stack);
            if ("BASE".equals(cofh)) return 401;
            if ("FULL".equals(cofh)) return 402;
        } catch (RuntimeException ignored) {}
        catch (LinkageError ignored) {}
        return 0;
    }
}
