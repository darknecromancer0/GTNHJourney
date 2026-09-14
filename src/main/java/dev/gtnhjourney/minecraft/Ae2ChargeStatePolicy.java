package dev.gtnhjourney.minecraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * AE2 powered tools use a continuously changing double-valued NBT field for charge. Journey treats that value as
 * runtime state: an empty item is the 0% endpoint and any verified positive charge proves ownership of the same item's
 * real 100% endpoint. Terminal search text is also transient UI state and must not create research variants.
 */
public final class Ae2ChargeStatePolicy {

    static final String CHARGE_KEY = "internalCurrentPower";
    static final String SEARCH_STRING_KEY = "searchString";
    private static final String AE2_PACKAGE_PREFIX = "appeng.";
    private static final String AE2_POWER_INTERFACE = "appeng.api.implementations.items.IAEItemPowerStorage";

    enum State {
        EXACT,
        BASE,
        FULL
    }

    private Ae2ChargeStatePolicy() {}

    public static ItemStack identityStack(ItemStack observed) {
        if (observed == null) return null;
        State state = classify(observed);
        if (state == State.BASE) return withoutCharge(observed);
        if (state == State.FULL) {
            ItemStack full = toFull(observed);
            return full == null ? observed.copy() : full;
        }
        return observed.copy();
    }

    public static List<ItemStack> expand(ItemStack observed) {
        if (observed == null || observed.getItem() == null) return Collections.emptyList();
        State state = classify(observed);
        if (state == State.EXACT) return Collections.singletonList(observed.copy());

        ItemStack base = withoutCharge(observed);
        base.stackSize = 1;
        if (state == State.BASE) return Collections.singletonList(base);

        ItemStack full = toFull(observed);
        if (full == null) return Collections.singletonList(observed.copy());
        full.stackSize = 1;
        List<ItemStack> endpoints = new ArrayList<ItemStack>(2);
        endpoints.add(base);
        endpoints.add(full);
        return Collections.unmodifiableList(endpoints);
    }

    static State classify(ItemStack observed) {
        if (observed == null || observed.getItem() == null || !isVerifiedAe2PoweredItem(observed.getItem())) {
            return State.EXACT;
        }
        double max = maxCharge(observed);
        if (!isFinitePositive(max)) return State.EXACT;

        NBTTagCompound tag = observed.getTagCompound();
        double current = tag != null && tag.hasKey(CHARGE_KEY) ? tag.getDouble(CHARGE_KEY) : 0.0D;
        if (Double.isNaN(current) || Double.isInfinite(current) || current < 0.0D) return State.EXACT;
        return current > 0.0D ? State.FULL : State.BASE;
    }

    static ItemStack withoutCharge(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.hasTagCompound()) return copy;
        NBTTagCompound tag = (NBTTagCompound) copy.getTagCompound().copy();
        tag.removeTag(CHARGE_KEY);
        tag.removeTag(SEARCH_STRING_KEY);
        if (tag.func_150296_c().isEmpty()) copy.setTagCompound(null);
        else copy.setTagCompound(tag);
        return copy;
    }

    static ItemStack toFull(ItemStack stack) {
        if (stack == null || stack.getItem() == null || !isVerifiedAe2PoweredItem(stack.getItem())) return null;
        double max = maxCharge(stack);
        if (!isFinitePositive(max)) return null;

        ItemStack copy = stack.copy();
        NBTTagCompound tag = copy.hasTagCompound() ? (NBTTagCompound) copy.getTagCompound().copy() : new NBTTagCompound();
        tag.setDouble(CHARGE_KEY, max);
        tag.removeTag(SEARCH_STRING_KEY);
        copy.setTagCompound(tag);
        return copy;
    }

    private static boolean isVerifiedAe2PoweredItem(Object item) {
        if (item == null) return false;
        Class<?> type = item.getClass();
        String name = type.getName();
        if (!(name.startsWith(AE2_PACKAGE_PREFIX) || implementsInterfaceNamed(type, AE2_POWER_INTERFACE))) return false;
        try {
            type.getMethod("getAEMaxPower", ItemStack.class);
            type.getMethod("getAECurrentPower", ItemStack.class);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }

    private static boolean implementsInterfaceNamed(Class<?> type, String interfaceName) {
        if (type == null) return false;
        for (Class<?> contract : type.getInterfaces()) {
            if (interfaceName.equals(contract.getName()) || implementsInterfaceNamed(contract, interfaceName)) return true;
        }
        return implementsInterfaceNamed(type.getSuperclass(), interfaceName);
    }

    private static double maxCharge(ItemStack stack) {
        try {
            Method method = stack.getItem().getClass().getMethod("getAEMaxPower", ItemStack.class);
            Object value = method.invoke(stack.getItem(), stack);
            return value instanceof Number ? ((Number) value).doubleValue() : -1.0D;
        } catch (ReflectiveOperationException ignored) {
            return -1.0D;
        } catch (RuntimeException ignored) {
            return -1.0D;
        } catch (LinkageError ignored) {
            return -1.0D;
        }
    }

    private static boolean isFinitePositive(double value) {
        return value > 0.0D && !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
