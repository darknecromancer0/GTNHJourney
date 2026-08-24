package dev.gtnhjourney.minecraft;

import java.lang.reflect.Method;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * GT5U electric charge semantics matching the established Journey policy: ordinary/partial charge collapses to the
 * base endpoint, while a verified FULL charge remains a distinct semantic state.
 */
public final class GtChargeStatePolicy {
    public static final String CHARGE_KEY = "GT.ItemCharge";
    private static final Class<?>[] VERIFIED_GT_BASES = loadVerifiedGtBases();

    enum State { EXACT, BASE, FULL }

    private GtChargeStatePolicy() {}

    /** Returns a copy suitable for identity calculation without mutating the observed stack. */
    public static ItemStack identityStack(ItemStack observed) {
        if (observed == null) return null;
        ItemStack copy = observed.copy();
        if (!ResearchCompatibilityOptions.normalizeGtChargeEndpoints()) return copy;
        State state = classify(observed);
        return state == State.BASE ? withoutCharge(copy) : copy;
    }

    public static boolean isVerifiedFull(ItemStack observed) {
        return ResearchCompatibilityOptions.normalizeGtChargeEndpoints() && classify(observed) == State.FULL;
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
        long fullThreshold = (long) Math.ceil(max - 0.000001D);
        return charge >= fullThreshold ? State.FULL : State.BASE;
    }

    static ItemStack withoutCharge(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.hasTagCompound()) return copy;
        NBTTagCompound tag = (NBTTagCompound) copy.getTagCompound().copy();
        tag.removeTag(CHARGE_KEY);
        if (tag.func_150296_c().isEmpty()) copy.setTagCompound(null);
        else copy.setTagCompound(tag);
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
        try { out.add(Class.forName(name, false, GtChargeStatePolicy.class.getClassLoader())); }
        catch (ClassNotFoundException ignored) {}
        catch (LinkageError ignored) {}
    }

    private static double maxCharge(ItemStack stack) {
        try {
            Method method = stack.getItem().getClass().getMethod("getMaxCharge", ItemStack.class);
            Object value = method.invoke(stack.getItem(), stack);
            return value instanceof Number ? ((Number) value).doubleValue() : -1.0D;
        } catch (ReflectiveOperationException ignored) {
            return -1.0D;
        } catch (RuntimeException ignored) {
            return -1.0D;
        }
    }
}
