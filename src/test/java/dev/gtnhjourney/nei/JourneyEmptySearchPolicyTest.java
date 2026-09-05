package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    public void filledContainerHiddenEmptyFalsePositiveIsRejected() {
        assertFalse(JourneyEmptySearchPolicy.resolveLiteralEmptyState(true, true, false, false));
        assertFalse(JourneyEmptySearchPolicy.resolveLiteralEmptyState(false, true, false, false));
    }

    @Test
    public void filledContainerVisibleEmptyNameStillRequiresNativeMatch() {
        assertTrue(JourneyEmptySearchPolicy.resolveLiteralEmptyState(true, true, false, true));
        assertFalse(JourneyEmptySearchPolicy.resolveLiteralEmptyState(false, true, false, true));
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
        return new ItemStack(new TestFluidContainerItem());
    }

    private static final class TestFluidContainerItem extends Item implements IFluidContainerItem {

        private TestFluidContainerItem() {
            setUnlocalizedName("journeyEmptyContainer");
        }

        @Override
        public FluidStack getFluid(ItemStack container) {
            return null;
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
