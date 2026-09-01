package dev.gtnhjourney.minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Optional IC2 electric-item endpoint semantics without a hard compile/runtime dependency on IC2. */
public final class Ic2ChargeStatePolicy {

    private static final String CHARGE_KEY = "charge";
    private static final String ELECTRIC_JETPACK = "IC2:itemArmorJetpackElectric";
    private static final Api API = Api.load();

    enum State {
        EXACT,
        BASE,
        FULL
    }

    private Ic2ChargeStatePolicy() {}

    public static String describe(ItemStack observed) {
        return classify(observed).name();
    }

    public static boolean isApiAvailable() {
        return API != null;
    }

    public static boolean isManagerReady() {
        if (API == null) return false;
        try {
            return API.managerField.get(null) != null;
        } catch (ReflectiveOperationException ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static ItemStack identityStack(ItemStack observed) {
        if (observed == null) return null;
        if (!ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()) return observed.copy();
        State state = classify(observed);
        if (state == State.FULL && collapseFullEndpoint(registryId(observed))) {
            ItemStack base = toBase(observed);
            return base == null ? observed.copy() : base;
        }
        if (state != State.BASE) return observed.copy();
        ItemStack base = toBase(observed);
        return base == null ? observed.copy() : base;
    }

    public static List<ItemStack> expand(ItemStack observed) {
        if (observed == null || observed.getItem() == null) return Collections.emptyList();
        if (!ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()) {
            ItemStack exact = observed.copy();
            exact.stackSize = 1;
            return Collections.singletonList(exact);
        }
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
            if (collapseFullEndpoint(registryId(observed))) return Collections.singletonList(base);
            List<ItemStack> out = new ArrayList<ItemStack>(2);
            out.add(base);
            out.add(exact);
            return Collections.unmodifiableList(out);
        }
        return Collections.singletonList(exact);
    }

    static State classify(ItemStack observed) {
        if (!ResearchCompatibilityOptions.normalizeIc2ChargeEndpoints()) return State.EXACT;
        if (observed == null || observed.getItem() == null || API == null) return State.EXACT;
        try {
            if (!API.electricItemClass.isInstance(observed.getItem())) return State.EXACT;
            Object manager = API.managerField.get(null);
            if (manager == null) return State.EXACT;
            double max = number(API.getMaxCharge.invoke(observed.getItem(), observed));
            double current = number(API.getCharge.invoke(manager, observed));
            if (!finitePositive(max) || Double.isNaN(current) || Double.isInfinite(current) || current < -0.000001D) {
                return State.EXACT;
            }
            if (current >= max - Math.max(0.000001D, max * 1.0E-9D)) return State.FULL;
            return State.BASE;
        } catch (ReflectiveOperationException ignored) {
            return State.EXACT;
        } catch (RuntimeException ignored) {
            return State.EXACT;
        }
    }

    static boolean collapseFullEndpoint(String itemId) {
        return ELECTRIC_JETPACK.equals(itemId);
    }

    private static String registryId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static ItemStack toBase(ItemStack observed) {
        if (observed == null || API == null) return null;
        try {
            if (!API.electricItemClass.isInstance(observed.getItem())) return null;
            Object manager = API.managerField.get(null);
            if (manager == null) return null;
            ItemStack copy = observed.copy();
            copy.stackSize = 1;
            double current = number(API.getCharge.invoke(manager, copy));
            if (current > 0.000001D) {
                API.discharge.invoke(manager, copy, Double.MAX_VALUE, Integer.MAX_VALUE, true, false, false);
                double remaining = number(API.getCharge.invoke(manager, copy));
                if (remaining > 0.000001D) return null;
            }
            copy.setItemDamage(ElectricItemDamagePolicy.emptyDamage(copy.getMaxDamage(), copy.getItemDamage()));
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
        }
    }

    private static boolean finitePositive(double value) {
        return value > 0.0D && !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.NaN;
    }

    private static final class Api {

        final Class<?> electricItemClass;
        final Field managerField;
        final Method getMaxCharge;
        final Method getCharge;
        final Method discharge;

        private Api(Class<?> electricItemClass, Field managerField, Method getMaxCharge, Method getCharge,
            Method discharge) {
            this.electricItemClass = electricItemClass;
            this.managerField = managerField;
            this.getMaxCharge = getMaxCharge;
            this.getCharge = getCharge;
            this.discharge = discharge;
        }

        static Api load() {
            try {
                Class<?> electricItemClass = Class.forName("ic2.api.item.IElectricItem");
                Class<?> electricItemHolder = Class.forName("ic2.api.item.ElectricItem");
                Class<?> managerClass = Class.forName("ic2.api.item.IElectricItemManager");
                return new Api(
                    electricItemClass,
                    electricItemHolder.getField("manager"),
                    electricItemClass.getMethod("getMaxCharge", ItemStack.class),
                    managerClass.getMethod("getCharge", ItemStack.class),
                    managerClass.getMethod(
                        "discharge",
                        ItemStack.class,
                        double.class,
                        int.class,
                        boolean.class,
                        boolean.class,
                        boolean.class));
            } catch (ReflectiveOperationException ignored) {
                return null;
            } catch (LinkageError ignored) {
                return null;
            }
        }
    }
}
