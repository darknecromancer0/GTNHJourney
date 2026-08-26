package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Narrowly strips GT++ Hand Pump fluid/init runtime payload while preserving real tool and charge identity. */
public final class TransientToolFluidPolicy {

    private static final String GT_PLUS_PLUS_PUMP = "gtPlusPlus.core.item.tool.misc.ItemGregtechPump";

    private TransientToolFluidPolicy() {}

    public static boolean isTransientFluidToolClassName(String className) {
        return GT_PLUS_PLUS_PUMP.equals(className);
    }

    static boolean isTransientRuntimeKey(String className, String key) {
        if (!isTransientFluidToolClassName(className) || key == null) return false;
        return "mInit".equals(key) || "mFluid".equals(key) || "mFluidAmount".equals(key) || "mMeta".equals(key)
            || "mCapacity".equals(key) || "capacityInit".equals(key);
    }

    public static void normalize(ItemStack stack, NBTTagCompound tag) {
        if (stack == null || stack.getItem() == null || tag == null) return;
        String className = stack.getItem().getClass().getName();
        if (!isTransientFluidToolClassName(className)) return;
        if (isTransientRuntimeKey(className, "mInit")) tag.removeTag("mInit");
        if (isTransientRuntimeKey(className, "mFluid")) tag.removeTag("mFluid");
        if (isTransientRuntimeKey(className, "mFluidAmount")) tag.removeTag("mFluidAmount");
        if (isTransientRuntimeKey(className, "mMeta")) tag.removeTag("mMeta");
        if (isTransientRuntimeKey(className, "mCapacity")) tag.removeTag("mCapacity");
        if (isTransientRuntimeKey(className, "capacityInit")) tag.removeTag("capacityInit");
    }
}
