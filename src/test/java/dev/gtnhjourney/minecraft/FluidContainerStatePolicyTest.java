package dev.gtnhjourney.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

public class FluidContainerStatePolicyTest {

    @Test
    public void oneMilliBucketProvesSingleFullEndpoint() {
        TestTankItem item = new TestTankItem(1000);
        ItemStack partial = new ItemStack(item, 1, 0);
        item.fill(partial, new FluidStack(FluidRegistry.WATER, 1), true);

        List<ItemStack> expanded = ResearchStateExpander.expand(partial);

        assertEquals(1, expanded.size());
        FluidStack full = item.getFluid(expanded.get(0));
        assertNotNull(full);
        assertEquals(1000, full.amount);
        assertEquals(FluidRegistry.WATER, full.getFluid());
    }

    @Test
    public void partialAmountsOfSameFluidHaveSameResearchIdentity() {
        TestTankItem item = new TestTankItem(8000);
        ItemStack one = new ItemStack(item, 1, 0);
        ItemStack sevenThousand = new ItemStack(item, 1, 0);
        item.fill(one, new FluidStack(FluidRegistry.WATER, 1), true);
        item.fill(sevenThousand, new FluidStack(FluidRegistry.WATER, 7000), true);

        ItemStack oneFull = ResearchStateExpander.expand(one).get(0);
        ItemStack sevenFull = ResearchStateExpander.expand(sevenThousand).get(0);

        assertEquals(ItemStackKeyFactory.from(oneFull), ItemStackKeyFactory.from(sevenFull));
        assertEquals(8000, item.getFluid(oneFull).amount);
        assertEquals(8000, item.getFluid(sevenFull).amount);
    }

    @Test
    public void differentFluidsRemainDifferentResearchStates() {
        TestTankItem item = new TestTankItem(1000);
        ItemStack water = new ItemStack(item, 1, 0);
        ItemStack lava = new ItemStack(item, 1, 0);
        item.fill(water, new FluidStack(FluidRegistry.WATER, 1), true);
        item.fill(lava, new FluidStack(FluidRegistry.LAVA, 1), true);

        ItemStack waterFull = ResearchStateExpander.expand(water).get(0);
        ItemStack lavaFull = ResearchStateExpander.expand(lava).get(0);

        assertFalse(ItemStackKeyFactory.from(waterFull).equals(ItemStackKeyFactory.from(lavaFull)));
    }

    @Test
    public void emptyContainerStaysEmpty() {
        TestTankItem item = new TestTankItem(1000);
        ItemStack empty = new ItemStack(item, 1, 0);

        List<ItemStack> expanded = ResearchStateExpander.expand(empty);

        assertEquals(1, expanded.size());
        assertTrue(item.getFluid(expanded.get(0)) == null);
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
            if (!root.hasKey("Fluid", 10)) return null;
            return FluidStack.loadFluidStackFromNBT(root.getCompoundTag("Fluid"));
        }

        @Override
        public int getCapacity(ItemStack container) {
            return capacity;
        }

        @Override
        public int fill(ItemStack container, FluidStack resource, boolean doFill) {
            if (container == null || resource == null || resource.getFluid() == null) return 0;
            FluidStack current = getFluid(container);
            if (current != null && !current.isFluidEqual(resource)) return 0;
            int currentAmount = current == null ? 0 : current.amount;
            int accepted = Math.min(capacity - currentAmount, resource.amount);
            if (accepted <= 0) return 0;
            if (doFill) {
                FluidStack stored = resource.copy();
                stored.amount = currentAmount + accepted;
                NBTTagCompound root = container.hasTagCompound() ? container.getTagCompound() : new NBTTagCompound();
                NBTTagCompound fluid = new NBTTagCompound();
                stored.writeToNBT(fluid);
                root.setTag("Fluid", fluid);
                container.setTagCompound(root);
            }
            return accepted;
        }

        @Override
        public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {
            FluidStack current = getFluid(container);
            if (current == null || maxDrain <= 0) return null;
            int drainedAmount = Math.min(maxDrain, current.amount);
            FluidStack drained = current.copy();
            drained.amount = drainedAmount;
            if (doDrain) {
                int left = current.amount - drainedAmount;
                if (left <= 0) {
                    container.getTagCompound().removeTag("Fluid");
                    if (container.getTagCompound().func_150296_c().isEmpty()) container.setTagCompound(null);
                } else {
                    current.amount = left;
                    NBTTagCompound fluid = new NBTTagCompound();
                    current.writeToNBT(fluid);
                    container.getTagCompound().setTag("Fluid", fluid);
                }
            }
            return drained;
        }
    }
}
