package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

public class FluidContainerStatePolicyTest {

    private static final Fluid WATER = new Fluid("journey_test_water");
    private static final Fluid LAVA = new Fluid("journey_test_lava");

    @Test
    public void oneMilliBucketProvesSingleFullEndpoint() {
        TestTankItem item = new TestTankItem(1000);
        ItemStack partial = new ItemStack(item, 1, 0);
        item.fill(partial, new FluidStack(WATER, 1), true);

        List<ItemStack> expanded = ResearchStateExpander.expand(partial);

        assertEquals(1, expanded.size());
        FluidStack full = item.getFluid(expanded.get(0));
        assertNotNull(full);
        assertEquals(1000, full.amount);
        assertEquals(WATER, full.getFluid());
    }

    @Test
    public void partialAmountsOfSameFluidHaveSameResearchIdentity() {
        TestTankItem item = new TestTankItem(8000);
        ItemStack one = new ItemStack(item, 1, 0);
        ItemStack sevenThousand = new ItemStack(item, 1, 0);
        item.fill(one, new FluidStack(WATER, 1), true);
        item.fill(sevenThousand, new FluidStack(WATER, 7000), true);

        ItemStack oneFull = ResearchStateExpander.expand(one).get(0);
        ItemStack sevenFull = ResearchStateExpander.expand(sevenThousand).get(0);

        assertEquals(ResearchNbtIdentity.canonicalize(oneFull), ResearchNbtIdentity.canonicalize(sevenFull));
        assertEquals(8000, item.getFluid(oneFull).amount);
        assertEquals(8000, item.getFluid(sevenFull).amount);
    }

    @Test
    public void differentFluidsRemainDifferentResearchStates() {
        TestTankItem item = new TestTankItem(1000);
        ItemStack water = new ItemStack(item, 1, 0);
        ItemStack lava = new ItemStack(item, 1, 0);
        item.fill(water, new FluidStack(WATER, 1), true);
        item.fill(lava, new FluidStack(LAVA, 1), true);

        ItemStack waterFull = ResearchStateExpander.expand(water).get(0);
        ItemStack lavaFull = ResearchStateExpander.expand(lava).get(0);

        assertFalse(ResearchNbtIdentity.canonicalize(waterFull).equals(ResearchNbtIdentity.canonicalize(lavaFull)));
    }

    @Test
    public void emptyContainerStaysEmpty() {
        TestTankItem item = new TestTankItem(1000);
        ItemStack empty = new ItemStack(item, 1, 0);

        List<ItemStack> expanded = ResearchStateExpander.expand(empty);

        assertEquals(1, expanded.size());
        assertNull(item.getFluid(expanded.get(0)));
    }

    private static final class TestTankItem extends Item implements IFluidContainerItem {
        private final int capacity;

        private TestTankItem(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public FluidStack getFluid(ItemStack container) {
            if (container == null || !container.hasTagCompound()) return null;
            NBTTagCompound root = container.getTagCompound();
            if (!root.hasKey("FluidKind", 8)) return null;
            Fluid fluid = "water".equals(root.getString("FluidKind")) ? WATER
                : "lava".equals(root.getString("FluidKind")) ? LAVA : null;
            if (fluid == null) return null;
            return new FluidStack(fluid, root.getInteger("Amount"));
        }

        @Override
        public int getCapacity(ItemStack container) {
            return capacity;
        }

        @Override
        public int fill(ItemStack container, FluidStack resource, boolean doFill) {
            if (container == null || resource == null || resource.getFluid() == null) return 0;
            FluidStack current = getFluid(container);
            if (current != null && current.getFluid() != resource.getFluid()) return 0;
            int currentAmount = current == null ? 0 : current.amount;
            int accepted = Math.min(capacity - currentAmount, resource.amount);
            if (accepted <= 0) return 0;
            if (doFill) {
                NBTTagCompound root = container.hasTagCompound() ? container.getTagCompound() : new NBTTagCompound();
                root.setString("FluidKind", resource.getFluid() == WATER ? "water" : "lava");
                root.setInteger("Amount", currentAmount + accepted);
                container.setTagCompound(root);
            }
            return accepted;
        }

        @Override
        public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {
            FluidStack current = getFluid(container);
            if (current == null || maxDrain <= 0) return null;
            int drainedAmount = Math.min(maxDrain, current.amount);
            FluidStack drained = new FluidStack(current.getFluid(), drainedAmount);
            if (doDrain) {
                int left = current.amount - drainedAmount;
                if (left <= 0) {
                    container.setTagCompound(null);
                } else {
                    container.getTagCompound().setInteger("Amount", left);
                }
            }
            return drained;
        }
    }
}
