package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Narrowly strips fluid payload from GT++ Hand Pumps while preserving their actual tool identity. */
public final class TransientToolFluidPolicy {

    private static final String GT_PLUS_PLUS_PUMP = "gtPlusPlus.core.item.tool.misc.ItemGregtechPump";

    private TransientToolFluidPolicy() {}

    public static boolean isTransientFluidToolClassName(String className) {
        return GT_PLUS_PLUS_PUMP.equals(className);
    }

    public static void normalize(ItemStack stack, NBTTagCompound tag) {
        if (stack == null || stack.getItem() == null || tag == null) return;
        if (!isTransientFluidToolClassName(stack.getItem().getClass().getName())) return;
        tag.removeTag("mFluid");
        tag.removeTag("mFluidAmount");
        tag.removeTag("mInit");
    }
}
