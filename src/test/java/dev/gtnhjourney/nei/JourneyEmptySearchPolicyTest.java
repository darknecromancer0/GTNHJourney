package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.junit.jupiter.api.Test;

public class JourneyEmptySearchPolicyTest {

    @Test
    public void ordinaryQueriesDoNotChangeNativeSearchResult() {
        ItemStack container = emptyContainer();
        assertTrue(JourneyEmptySearchPolicy.resolveSearchMatch("water", container, true));
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("water", container, false));
    }

    @Test
    public void emptyQueryRejectsFilledFluidContainerTooltipFalsePositive() {
        ItemStack filled = filledContainer();
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("empty", filled, true));
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("\"empty\"", filled, true));
    }

    @Test
    public void emptyQueryAddsHiddenAliasToActuallyEmptyFluidContainers() {
        ItemStack empty = emptyContainer();
        assertTrue(JourneyEmptySearchPolicy.resolveSearchMatch("empty", empty, false));
        assertTrue(JourneyEmptySearchPolicy.resolveSearchMatch("\"empty\"", empty, false));
    }

    @Test
    public void prefixedEmptySearchRemainsCompletelyNative() {
        ItemStack empty = emptyContainer();
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("@empty", empty, false));
        assertTrue(JourneyEmptySearchPolicy.resolveSearchMatch("@empty", empty, true));
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("#empty", empty, false));
    }

    private static ItemStack emptyContainer() {
        return new ItemStack(new TestFluidContainerItem(null));
    }

    private static ItemStack filledContainer() {
        return new ItemStack(new TestFluidContainerItem(new FluidStack(new Fluid("journey_test_fluid"), 1000)));
    }

    private static final class TestFluidContainerItem extends Item implements IFluidContainerItem {

        private final FluidStack fluid;

        private TestFluidContainerItem(FluidStack fluid) {
            this.fluid = fluid;
            setUnlocalizedName(fluid == null ? "journeyEmptyContainer" : "journeyFilledContainer");
        }

        @Override
        public FluidStack getFluid(ItemStack container) {
            return fluid == null ? null : fluid.copy();
        }

        @Override
        public int getCapacity(ItemStack container) {
            return 1000;
        }

        @Override
        public int fill(ItemStack container, FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {
            return null;
        }
    }
}
