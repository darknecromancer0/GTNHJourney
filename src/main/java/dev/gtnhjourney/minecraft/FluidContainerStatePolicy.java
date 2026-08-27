package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/** Collapses real Forge fluid containers to EMPTY or one FULL endpoint per contained fluid. */
public final class FluidContainerStatePolicy {

    private FluidContainerStatePolicy() {}

    static int targetFullAmount(int observedAmount, int capacity) {
        return observedAmount > 0 && capacity > 0 ? capacity : 0;
    }

    /**
     * Returns a FULL equivalent when the observed stack is a proven Forge fluid container containing at least 1 mB.
     * Returns null for empty/non-container/unsafe stacks so callers can preserve their existing exact semantics.
     */
    public static ItemStack fullEquivalent(ItemStack observed) {
        if (observed == null || observed.getItem() == null) return null;
        if (!(observed.getItem() instanceof IFluidContainerItem)) return null;

        try {
            IFluidContainerItem container = (IFluidContainerItem) observed.getItem();
            FluidStack fluid = container.getFluid(observed);
            if (fluid == null || fluid.getFluid() == null || fluid.amount <= 0) return null;

            int capacity = container.getCapacity(observed);
            int target = targetFullAmount(fluid.amount, capacity);
            if (target <= 0 || fluid.amount > target) return null;

            ItemStack full = observed.copy();
            full.stackSize = 1;
            if (fluid.amount < target) {
                FluidStack topUp = fluid.copy();
                int needed = target - fluid.amount;
                topUp.amount = needed;
                // IFluidContainerItem implementations disagree on whether fill() reports the accepted delta or the
                // resulting contained amount. The postcondition below is authoritative, so only require a positive
                // mutation result here and verify the actual FULL state afterwards.
                int accepted = container.fill(full, topUp, true);
                if (accepted <= 0) return null;
            }

            FluidStack result = container.getFluid(full);
            if (result == null || result.getFluid() == null || result.amount != target) return null;
            if (!result.isFluidEqual(fluid)) return null;
            return full;
        } catch (RuntimeException | LinkageError unsafeContainer) {
            return null;
        }
    }
}
