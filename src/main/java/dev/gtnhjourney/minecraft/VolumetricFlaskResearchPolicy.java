package dev.gtnhjourney.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.common.items.ItemVolumetricFlask;

/**
 * The four GT/GT++ volumetric flask types share ItemVolumetricFlask. Observing a filled flask proves the exact
 * amount actually held and also grants a separate 1000 mB preset of the same fluid in the same flask type.
 * No liquid is created in the player's inventory; only Journey's retrievable research templates are expanded.
 */
public final class VolumetricFlaskResearchPolicy {

    private static final int STANDARD_AMOUNT_MB = 1000;

    private VolumetricFlaskResearchPolicy() {}

    static int standardAmount(int observedAmount, int maxCapacity) {
        return observedAmount > 0 && maxCapacity >= STANDARD_AMOUNT_MB ? STANDARD_AMOUNT_MB : 0;
    }

    public static List<ItemStack> expand(ItemStack observed) {
        if (observed == null || !(observed.getItem() instanceof ItemVolumetricFlask)) {
            return Collections.emptyList();
        }
        ItemStack exact = observed.copy();
        exact.stackSize = 1;
        try {
            ItemVolumetricFlask flask = (ItemVolumetricFlask) exact.getItem();
            FluidStack original = flask.getFluid(exact);
            int capacity = flask.getCapacity(exact);
            if (original == null || original.getFluid() == null || original.amount <= 0 || capacity <= 0
                || original.amount > capacity) return Collections.emptyList();

            int standard = standardAmount(original.amount, flask.getMaxCapacity());
            if (standard == 0 || (capacity == standard && original.amount == standard)) {
                return Collections.singletonList(exact);
            }

            ItemStack fullLitre = exact.copy();
            flask.setCapacity(fullLitre, standard);
            FluidStack litre = original.copy();
            litre.amount = standard;
            flask.setFluid(fullLitre, litre);

            // Fail closed if a runtime flask implementation cannot actually represent the new preset.
            FluidStack verified = flask.getFluid(fullLitre);
            if (flask.getCapacity(fullLitre) != standard || verified == null
                || !verified.isFluidEqual(original) || verified.amount != standard) {
                return Collections.singletonList(exact);
            }

            List<ItemStack> endpoints = new ArrayList<ItemStack>(2);
            endpoints.add(exact);
            endpoints.add(fullLitre);
            return Collections.unmodifiableList(endpoints);
        } catch (RuntimeException | LinkageError optionalModFailure) {
            return Collections.singletonList(exact);
        }
    }
}
