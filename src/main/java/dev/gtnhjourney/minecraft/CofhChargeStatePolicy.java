package dev.gtnhjourney.minecraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Optional CoFH RF item endpoint semantics without a hard dependency on CoFHCore.
 *
 * <p>
 * The owning item must implement {@code cofh.api.energy.IEnergyContainerItem}. Current and maximum energy are read
 * through that API and the base endpoint is produced by extracting energy from a copy. Arbitrary NBT keys named
 * {@code Energy} are never interpreted by Journey.
 * </p>
 */
public final class CofhChargeStatePolicy {

    private static final Api API = Api.load();
    /** Safety ceiling for pathological implementations. Normal RF items should need far fewer passes. */
    private static final int MAX_DRAIN_PASSES = 4096;

    enum State {
        EXACT,
        BASE,
        FULL
    }

    private CofhChargeStatePolicy() {}

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
        if (!ResearchCompatibilityOptions.normalizeCofhChargeEndpoints() || observed == null
            || observed.getItem() == null
            || API == null) return State.EXACT;
        try {
            if (!API.energyItemClass.isInstance(observed.getItem())) return State.EXACT;
            int max = number(API.getMaxEnergyStored.invoke(observed.getItem(), observed));
            int current = number(API.getEnergyStored.invoke(observed.getItem(), observed));
            if (max <= 0 || current < 0) return State.EXACT;
            if (current > 0) {
                // Prove the item is actually extractable without performing a full drain just to classify it.
                int simulated = number(API.extractEnergy.invoke(observed.getItem(), observed, Integer.MAX_VALUE, true));
                if (simulated <= 0) return State.EXACT;
            }
            return current >= max ? State.FULL : State.BASE;
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
            if (!API.energyItemClass.isInstance(observed.getItem())) return null;
            ItemStack copy = observed.copy();
            copy.stackSize = 1;
            int remaining = number(API.getEnergyStored.invoke(copy.getItem(), copy));
            if (remaining < 0) return null;
            for (int pass = 0; remaining > 0 && pass < MAX_DRAIN_PASSES; pass++) {
                int extracted = number(API.extractEnergy.invoke(copy.getItem(), copy, Integer.MAX_VALUE, false));
                int after = number(API.getEnergyStored.invoke(copy.getItem(), copy));
                if (extracted <= 0 || after < 0 || after >= remaining) return null;
                remaining = after;
            }
            if (remaining != 0) return null;
            return copy;
        } catch (ReflectiveOperationException ignored) {
            return null;
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static int number(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    private static final class Api {

        final Class<?> energyItemClass;
        final Method extractEnergy;
        final Method getEnergyStored;
        final Method getMaxEnergyStored;

        private Api(Class<?> energyItemClass, Method extractEnergy, Method getEnergyStored, Method getMaxEnergyStored) {
            this.energyItemClass = energyItemClass;
            this.extractEnergy = extractEnergy;
            this.getEnergyStored = getEnergyStored;
            this.getMaxEnergyStored = getMaxEnergyStored;
        }

        static Api load() {
            try {
                Class<?> energyItemClass = Class.forName("cofh.api.energy.IEnergyContainerItem");
                return new Api(
                    energyItemClass,
                    energyItemClass.getMethod("extractEnergy", ItemStack.class, int.class, boolean.class),
                    energyItemClass.getMethod("getEnergyStored", ItemStack.class),
                    energyItemClass.getMethod("getMaxEnergyStored", ItemStack.class));
            } catch (ReflectiveOperationException ignored) {
                return null;
            } catch (LinkageError ignored) {
                return null;
            }
        }
    }
}
