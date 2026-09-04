package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.ItemStack;

/** Converts a real Minecraft stack into the exact v0.1 Journey identity. */
public final class ItemStackKeyFactory {

    private ItemStackKeyFactory() {}

    public static ResearchKey from(ItemStack stack) {
        try {
            return fromUnchecked(stack);
        } catch (IllegalArgumentException knownIdentityFailure) {
            throw knownIdentityFailure;
        } catch (RuntimeException | LinkageError optionalIntegrationFailure) {
            throw new IllegalArgumentException(
                "item identity failed because a modded stack could not be inspected safely",
                optionalIntegrationFailure);
        }
    }

    private static ResearchKey fromUnchecked(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("stack and item must not be null");
        }

        ItemStack canonicalInput = ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()
            ? Ic2LegacyBatteryAliasPolicy.identityStack(stack) : stack.copy();
        if (canonicalInput == null || canonicalInput.getItem() == null) {
            throw new IllegalArgumentException("IC2 alias identity produced no item");
        }

        GtChargeStatePolicy.State gtChargeState = ResearchCompatibilityOptions.normalizeGtChargeEndpoints()
            ? GtChargeStatePolicy.classify(canonicalInput) : GtChargeStatePolicy.State.EXACT;
        ItemStack identityStack;
        if (gtChargeState != GtChargeStatePolicy.State.EXACT) {
            identityStack = GtChargeStatePolicy.identityStack(canonicalInput);
        } else {
            OpenComputersChargeStatePolicy.State ocChargeState = OpenComputersChargeStatePolicy.classify(canonicalInput);
            if (ocChargeState != OpenComputersChargeStatePolicy.State.EXACT) {
                identityStack = OpenComputersChargeStatePolicy.identityStack(canonicalInput);
            } else {
                Ic2ChargeStatePolicy.State ic2ChargeState = ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()
                    ? Ic2ChargeStatePolicy.classify(canonicalInput) : Ic2ChargeStatePolicy.State.EXACT;
                identityStack = ic2ChargeState != Ic2ChargeStatePolicy.State.EXACT
                    ? Ic2ChargeStatePolicy.identityStack(canonicalInput)
                    : CofhChargeStatePolicy.identityStack(canonicalInput);
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

        String rawItemId = id.modId + ":" + id.name;
        String aliasedItemId = KnownResearchItemAliasPolicy.canonicalItemId(rawItemId);
        String itemId = GalacticraftCanisterStatePolicy.canonicalItemId(aliasedItemId, identityStack.getItemDamage());
        String nbt = ResearchNbtIdentity.canonicalize(identityStack, itemId);
        int meta = VanillaMetadataPolicy.canonicalMeta(itemId, researchMeta(itemId, identityStack));
        return new ResearchKey(itemId, meta, nbt);
    }

    private static int researchMeta(String itemId, ItemStack stack) {
        if (GalacticraftOxygenTankStatePolicy.matches(itemId)) {
            return GalacticraftOxygenTankStatePolicy.canonicalMeta(itemId, stack.getItemDamage());
        }
        if (GalacticraftCanisterStatePolicy.matches(itemId)) {
            return GalacticraftCanisterStatePolicy.canonicalMeta(itemId, stack.getItemDamage());
        }
        if (ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()
            && Ic2ChargeStatePolicy.classify(stack) != Ic2ChargeStatePolicy.State.EXACT) {
            return stack.getItemDamage();
        }
        try {
            if (stack.getItem().isDamageable() && !stack.getItem().getHasSubtypes()) return 0;
        } catch (RuntimeException ignored) {}
        return stack.getItemDamage();
    }
}
