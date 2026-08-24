package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import dev.gtnhjourney.research.ResearchKey;

/** Converts a real Minecraft stack into the exact v0.1 Journey identity. */
public final class ItemStackKeyFactory {

    private ItemStackKeyFactory() {}

    public static ResearchKey from(ItemStack stack) {
        try {
            return fromUnchecked(stack);
        } catch (LinkageError optionalIntegrationFailure) {
            throw new IllegalArgumentException(
                "item identity failed because an optional integration is unavailable",
                optionalIntegrationFailure);
        }
    }

    private static ResearchKey fromUnchecked(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("stack and item must not be null");
        }

        // Semantic normalization may legitimately transform an electric stack into its base/empty representation.
        // Registry id and metadata therefore belong to the normalized stack too, not to the pre-normalization input.
        GtChargeStatePolicy.State gtChargeState = ResearchCompatibilityOptions.normalizeGtChargeEndpoints()
            ? GtChargeStatePolicy.classify(stack)
            : GtChargeStatePolicy.State.EXACT;
        ItemStack identityStack;
        if (gtChargeState != GtChargeStatePolicy.State.EXACT) {
            identityStack = GtChargeStatePolicy.identityStack(stack);
        } else {
            Ic2ChargeStatePolicy.State ic2ChargeState = ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()
                ? Ic2ChargeStatePolicy.classify(stack)
                : Ic2ChargeStatePolicy.State.EXACT;
            if (ic2ChargeState != Ic2ChargeStatePolicy.State.EXACT) {
                identityStack = Ic2ChargeStatePolicy.identityStack(stack);
            } else {
                OpenComputersChargeStatePolicy.State ocChargeState = OpenComputersChargeStatePolicy.classify(stack);
                identityStack = ocChargeState != OpenComputersChargeStatePolicy.State.EXACT
                    ? OpenComputersChargeStatePolicy.identityStack(stack)
                    : CofhChargeStatePolicy.identityStack(stack);
            }
        }
        if (identityStack == null || identityStack.getItem() == null) {
            throw new IllegalArgumentException("semantic identity produced no item");
        }

        UniqueIdentifier id;
        try {
            id = GameRegistry.findUniqueIdentifierFor(identityStack.getItem());
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("item registry lookup failed: " + identityStack.getItem(), failure);
        }
        if (id == null || id.modId == null || id.name == null) {
            throw new IllegalArgumentException("item has no Forge registry identifier: " + identityStack.getItem());
        }

        String nbt = ResearchNbtIdentity.canonicalize(identityStack);
        int meta = researchMeta(identityStack);
        return new ResearchKey(id.modId + ":" + id.name, meta, nbt);
    }

    private static int researchMeta(ItemStack stack) {
        // Legacy IC2 may encode electric state in ItemStack.damage itself. Once the stack is proven to participate in
        // IC2 charge endpoint semantics, preserve the manager-produced damage value instead of mistaking it for wear.
        if (ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()
            && Ic2ChargeStatePolicy.classify(stack) != Ic2ChargeStatePolicy.State.EXACT) {
            return stack.getItemDamage();
        }

        // In 1.7.10 ItemStack.damage doubles as both metadata and durability. Only collapse it when the Item itself
        // declares classic durability semantics and no subtypes. GT meta-items explicitly have subtypes, so their
        // tool/part IDs remain intact.
        try {
            if (stack.getItem()
                .isDamageable()
                && !stack.getItem()
                    .getHasSubtypes())
                return 0;
        } catch (RuntimeException ignored) {}
        return stack.getItemDamage();
    }
}
