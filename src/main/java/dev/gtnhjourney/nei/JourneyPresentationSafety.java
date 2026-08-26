package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Produces renderer-safe client-only copies without changing the authoritative retrieval template. */
final class JourneyPresentationSafety {

    private static final String VOLUMETRIC_FLASK = "gregtech.common.items.ItemVolumetricFlask";

    private JourneyPresentationSafety() {}

    static ItemStack forNei(ItemStack original) {
        if (original == null || original.getItem() == null) return original;
        ItemStack copy = original.copy();
        copy.stackSize = 1;
        if (!isVolumetricFlask(copy) || !copy.hasTagCompound() || !copy.getTagCompound().hasKey("Fluid", 10)) {
            return copy;
        }
        if (!hasUnsafeFluidIcon(copy)) return copy;
        copy.setTagCompound(sanitizedFlaskTag(copy.getTagCompound()));
        return copy;
    }

    static NBTTagCompound sanitizedFlaskTag(NBTTagCompound original) {
        if (original == null) return null;
        NBTTagCompound out = (NBTTagCompound) original.copy();
        out.removeTag("Fluid");
        return out.func_150296_c().isEmpty() ? null : out;
    }

    static boolean hasUnsafeFluidIcon(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound() || !stack.getTagCompound().hasKey("Fluid", 10)) return false;
        try {
            FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(stack.getTagCompound().getCompoundTag("Fluid"));
            if (fluidStack == null) return false;
            Fluid fluid = fluidStack.getFluid();
            return fluid == null || fluid.getIcon(fluidStack) == null;
        } catch (Throwable unsafeRendererState) {
            return true;
        }
    }

    static boolean isVolumetricFlask(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        Class<?> type = stack.getItem().getClass();
        while (type != null) {
            if (VOLUMETRIC_FLASK.equals(type.getName())) return true;
            type = type.getSuperclass();
        }
        return false;
    }
}
