package dev.gtnhjourney.minecraft;

import java.lang.reflect.Method;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** GT5U electric items expose only two Journey charge endpoints: empty and full. */
public final class GtChargeStatePolicy {

    public static final String CHARGE_KEY = "GT.ItemCharge";
    private static final Class<?>[] VERIFIED_GT_BASES = loadVerifiedGtBases();

    enum State {
        EXACT,
        BASE,
        FULL
    }

    private GtChargeStatePolicy() {}

    /** Returns a canonical empty/full copy suitable for identity calculation without mutating the observed stack. */
    public static ItemStack identityStack(ItemStack observed) {
        if (observed == null) return null;
        ItemStack copy = observed.copy();
        if (!ResearchCompatibilityOptions.normalizeGtChargeEndpoints()) return copy;
        State state = classify(observed);
        if (state == State.BASE) return withoutCharge(copy);
        if (state == State.FULL) {
            ItemStack full = toFull(copy);
            return full == null ? copy : full;
        }
        return copy;
    }

    /** True only when the physically observed GT stack itself is already at its maximum charge. */
    public static boolean isVerifiedFull(ItemStack observed) {
        if (!ResearchCompatibilityOptions.normalizeGtChargeEndpoints() || observed == null || !observed.hasTagCompound()) {
            return false;
        }
        double max = maxCharge(observed);
        if (!(max > 0.0D) || Double.isNaN(max) || Double.isInfinite(max)) return false;
        NBTTagCompound tag = observed.getTagCompound();
        if (tag == null || !tag.hasKey(CHARGE_KEY)) return false;
        long fullThreshold = (long) Math.ceil(max - 0.000001D);
        return tag.getLong(CHARGE_KEY) >= fullThreshold;
    }

    public static String describe(ItemStack observed) {
        return classify(observed).name();
    }

    static State classify(ItemStack observed) {
        if (observed == null || observed.getItem() == null || !observed.hasTagCompound()) return State.EXACT;
        if (!isVerifiedGtItem(observed.getItem())) return State.EXACT;
        NBTTagCompound tag = observed.getTagCompound();
        if (tag == null || !tag.hasKey(CHARGE_KEY)) return State.EXACT;

        double max = maxCharge(observed);
        if (!(max > 0.0D) || Double.isNaN(max) || Double.isInfinite(max)) return State.EXACT;
        long charge = tag.getLong(CHARGE_KEY);
        if (charge < 0L) return State.EXACT;
        return charge > 0L ? State.FULL : State.BASE;
    }

    static ItemStack withoutCharge(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.hasTagCompound()) return copy;
        NBTTagCompound tag = (NBTTagCompound) copy.getTagCompound()
            .copy();
        tag.removeTag(CHARGE_KEY);
        if (tag.func_150296_c()
            .isEmpty()) copy.setTagCompound(null);
        else copy.setTagCompound(tag);
        return copy;
    }

    static ItemStack toFull(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        double max = maxCharge(stack);
        if (!(max > 0.0D) || Double.isNaN(max) || Double.isInfinite(max)) return null;
        long fullCharge = (long) Math.ceil(max - 0.000001D);
        if (fullCharge <= 0L) return null;

        ItemStack copy = stack.copy();
        NBTTagCompound tag = copy.hasTagCompound() ? (NBTTagCompound) copy.getTagCompound().copy() : new NBTTagCompound();
        tag.setLong(CHARGE_KEY, fullCharge);
        copy.setTagCompound(tag);
        return copy;
    }

    private static boolean isVerifiedGtItem(Object item) {
        if (item == null) return false;
        // Fail closed outside a verified GT runtime. A foreign item carrying the same NBT key must remain exact.
        if (VERIFIED_GT_BASES.length == 0) return false;
        for (Class<?> type : VERIFIED_GT_BASES) if (type.isInstance(item)) return true;
        return false;
    }

    private static Class<?>[] loadVerifiedGtBases() {
        java.util.ArrayList<Class<?>> out = new java.util.ArrayList<Class<?>>(2);
        addIfPresent(out, "gregtech.api.items.MetaBaseItem");
        addIfPresent(out, "gtPlusPlus.xmod.gregtech.api.items.GTMetaItemBase");
        return out.toArray(new Class<?>[out.size()]);
    }

    private static void addIfPresent(java.util.List<Class<?>> out, String name) {
        try {
            out.add(Class.forName(name, false, GtChargeStatePolicy.class.getClassLoader()));
        } catch (ClassNotFoundException ignored) {} catch (LinkageError ignored) {}
    }

    private static double maxCharge(ItemStack stack) {
        try {
            Method method = stack.getItem()
                .getClass()
                .getMethod("getMaxCharge", ItemStack.class);
            Object value = method.invoke(stack.getItem(), stack);
            return value instanceof Number ? ((Number) value).doubleValue() : -1.0D;
        } catch (ReflectiveOperationException ignored) {
            return -1.0D;
        } catch (RuntimeException ignored) {
            return -1.0D;
        }
    }
}
