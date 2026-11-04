package dev.gtnhjourney.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/**
 * GT and GT++ volumetric flasks use the same runtime item implementation. A real filled flask proves both the
 * exact held amount and a separate 1000 mB preset, with the same fluid and same flask type. Forge fluid API and
 * guarded reflection avoid introducing a hard GregTech class-loading dependency into Journey.
 */
public final class VolumetricFlaskResearchPolicy {

    private static final int STANDARD_AMOUNT_MB = 1000;
    private static final String GT_FLASK_CLASS = "gregtech.common.items.ItemVolumetricFlask";

    private VolumetricFlaskResearchPolicy() {}

    /** Cheap identity prefilter for old research entries: avoid deserializing every stored item on load. */
    public static boolean mayBeFlaskId(String itemId) {
        if (itemId == null) return false;
        String id = itemId.toLowerCase(Locale.ROOT);
        return id.contains("volumetric_flask") || id.contains("klein_bottle");
    }

    static int standardAmount(int observedAmount, int maxCapacity) {
        return observedAmount > 0 && maxCapacity >= STANDARD_AMOUNT_MB ? STANDARD_AMOUNT_MB : 0;
    }

    public static List<ItemStack> expand(ItemStack observed) {
        if (observed == null || observed.getItem() == null
            || !(observed.getItem() instanceof IFluidContainerItem)
            || !GT_FLASK_CLASS.equals(observed.getItem().getClass().getName())) {
            return Collections.emptyList();
        }
        ItemStack exact = observed.copy();
        exact.stackSize = 1;
        try {
            IFluidContainerItem container = (IFluidContainerItem) exact.getItem();
            FluidStack original = container.getFluid(exact);
            int capacity = container.getCapacity(exact);
            if (original == null || original.getFluid() == null || original.amount <= 0 || capacity <= 0
                || original.amount > capacity) return Collections.emptyList();

            // The full configurable flask capacity is different from the player's current Capacity NBT setting.
            Object maxValue = exact.getItem().getClass().getMethod("getMaxCapacity").invoke(exact.getItem());
            if (!(maxValue instanceof Number)) return Collections.singletonList(exact);
            int standard = standardAmount(original.amount, ((Number) maxValue).intValue());
            if (standard == 0 || (capacity == standard && original.amount == standard)) {
                return Collections.singletonList(exact);
            }

            ItemStack fullLitre = exact.copy();
            FluidStack litre = original.copy();
            litre.amount = standard;
            NBTTagCompound tag = fullLitre.hasTagCompound()
                ? (NBTTagCompound) fullLitre.getTagCompound().copy() : new NBTTagCompound();
            tag.setInteger("Capacity", standard);
            tag.setTag("Fluid", litre.writeToNBT(new NBTTagCompound()));
            fullLitre.setTagCompound(tag);

            // Accept only a preset the real container implementation recognizes, with the exact original fluid.
            FluidStack verified = container.getFluid(fullLitre);
            if (container.getCapacity(fullLitre) != standard || verified == null
                || !verified.isFluidEqual(original) || verified.amount != standard) {
                return Collections.singletonList(exact);
            }
            List<ItemStack> endpoints = new ArrayList<ItemStack>(2);
            endpoints.add(exact);
            endpoints.add(fullLitre);
            return Collections.unmodifiableList(endpoints);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError incompatibleRuntime) {
            return Collections.singletonList(exact);
        }
    }
}
