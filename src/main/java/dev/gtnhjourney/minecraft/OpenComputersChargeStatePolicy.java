package dev.gtnhjourney.minecraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * OpenComputers hover-boots energy endpoints using the item's own public charge API through reflection.
 * Partial charge collapses to BASE; a genuinely observed maximum charge proves BASE + FULL.
 */
public final class OpenComputersChargeStatePolicy {

    private static final String CHARGE_KEY = "oc:charge";
    private static final Api API = Api.load();

    enum State {
        EXACT,
        BASE,
        FULL
    }

    private OpenComputersChargeStatePolicy() {}

    public static boolean isApiAvailable() {
        return API != null;
    }

    public static String describe(ItemStack observed) {
        return classify(observed).name();
    }

    public static ItemStack identityStack(ItemStack observed) {
        if (observed == null) return null;
        State state = classify(observed);
        if (state != State.BASE) return observed.copy();
        ItemStack base = toBase(observed);
        return base == null ? observed.copy() : base;
    }

    public static List<ItemStack> expand(ItemStack observed) {
        if (observed == null || observed.getItem() == null) return Collections.emptyList();
        ItemStack exact = observed.copy();
        exact.stackSize = 1;
        State state = classify(observed);
        if (state == State.BASE) {
            ItemStack base = toBase(observed);
            if (base == null) return Collections.singletonList(exact);
            base.stackSize = 1;
            return Collections.singletonList(base);
        }
        if (state == State.FULL) {
            ItemStack base = toBase(observed);
            if (base == null) return Collections.singletonList(exact);
            base.stackSize = 1;
            List<ItemStack> out = new ArrayList<ItemStack>(2);
            out.add(base);
            out.add(exact);
            return Collections.unmodifiableList(out);
        }
        return Collections.singletonList(exact);
    }

    static State classify(ItemStack observed) {
        if (observed == null || observed.getItem() == null || API == null) return State.EXACT;
        try {
            if (!API.hoverBootsClass.isInstance(observed.getItem())) return State.EXACT;
            double max = number(API.maxCharge.invoke(observed.getItem(), observed));
            double current = number(API.getCharge.invoke(observed.getItem(), observed));
            if (!finitePositive(max) || Double.isNaN(current) || Double.isInfinite(current) || current < -0.000001D) {
                return State.EXACT;
            }
            return current >= max - Math.max(0.000001D, max * 1.0E-9D) ? State.FULL : State.BASE;
        } catch (ReflectiveOperationException ignored) {
            return State.EXACT;
        } catch (RuntimeException ignored) {
            return State.EXACT;
        } catch (LinkageError ignored) {
            return State.EXACT;
        }
    }

    private static ItemStack toBase(ItemStack observed) {
        if (observed == null || observed.getItem() == null || API == null) return null;
        try {
            if (!API.hoverBootsClass.isInstance(observed.getItem())) return null;
            ItemStack copy = observed.copy();
            copy.stackSize = 1;
            API.setCharge.invoke(copy.getItem(), copy, 0.0D);
            // HoverBootsData persists zero as oc:charge=0. Zero is the implicit default, so remove only this verified
            // key.
            if (copy.hasTagCompound()) {
                NBTTagCompound tag = (NBTTagCompound) copy.getTagCompound()
                    .copy();
                tag.removeTag(CHARGE_KEY);
                if (tag.func_150296_c()
                    .isEmpty()) copy.setTagCompound(null);
                else copy.setTagCompound(tag);
            }
            return copy;
        } catch (ReflectiveOperationException ignored) {
            return null;
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static boolean finitePositive(double value) {
        return value > 0.0D && !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.NaN;
    }

    private static final class Api {

        final Class<?> hoverBootsClass;
        final Method maxCharge;
        final Method getCharge;
        final Method setCharge;

        private Api(Class<?> hoverBootsClass, Method maxCharge, Method getCharge, Method setCharge) {
            this.hoverBootsClass = hoverBootsClass;
            this.maxCharge = maxCharge;
            this.getCharge = getCharge;
            this.setCharge = setCharge;
        }

        static Api load() {
            try {
                Class<?> hoverBootsClass = Class.forName("li.cil.oc.common.item.HoverBoots");
                return new Api(
                    hoverBootsClass,
                    hoverBootsClass.getMethod("maxCharge", ItemStack.class),
                    hoverBootsClass.getMethod("getCharge", ItemStack.class),
                    hoverBootsClass.getMethod("setCharge", ItemStack.class, double.class));
            } catch (ReflectiveOperationException ignored) {
                return null;
            } catch (LinkageError ignored) {
                return null;
            }
        }
    }
}
