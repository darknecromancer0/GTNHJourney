package dev.gtnhjourney.minecraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/** Optional CoFH RF item endpoint semantics without a hard dependency on CoFHCore. */
public final class CofhChargeStatePolicy {

    private static final Api API = Api.load();
    /** Safety ceiling for pathological implementations. Normal RF items should need far fewer passes. */
    private static final int MAX_TRANSFER_PASSES = 4096;

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
        if (state == State.BASE) {
            ItemStack base = toBase(observed);
            return base == null ? observed.copy() : base;
        }
        if (state == State.FULL) {
            ItemStack full = toFull(observed);
            return full == null ? observed.copy() : full;
        }
        return observed.copy();
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
            ItemStack full = toFull(observed);
            if (base == null || full == null) return Collections.singletonList(exact);
            base.stackSize = 1;
            full.stackSize = 1;
            List<ItemStack> out = new ArrayList<ItemStack>(2);
            out.add(base);
            out.add(full);
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
            boolean extractable = true;
            if (current > 0) {
                // Prove this is a real extractable energy state before treating a numeric value as charge semantics.
                int simulated = number(API.extractEnergy.invoke(observed.getItem(), observed, Integer.MAX_VALUE, true));
                extractable = simulated > 0;
            }
            ChargeEndpointClassifier.State endpoint = ChargeEndpointClassifier.classify(current, max, extractable);
            if (endpoint == ChargeEndpointClassifier.State.FULL) return State.FULL;
            if (endpoint == ChargeEndpointClassifier.State.BASE) return State.BASE;
            return State.EXACT;
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
            for (int pass = 0; remaining > 0 && pass < MAX_TRANSFER_PASSES; pass++) {
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

    private static ItemStack toFull(ItemStack observed) {
        if (observed == null || observed.getItem() == null || API == null) return null;
        try {
            if (!API.energyItemClass.isInstance(observed.getItem())) return null;
            ItemStack copy = observed.copy();
            copy.stackSize = 1;
            int max = number(API.getMaxEnergyStored.invoke(copy.getItem(), copy));
            int current = number(API.getEnergyStored.invoke(copy.getItem(), copy));
            if (max <= 0 || current < 0 || current > max) return null;
            for (int pass = 0; current < max && pass < MAX_TRANSFER_PASSES; pass++) {
                int received = number(API.receiveEnergy.invoke(copy.getItem(), copy, Integer.MAX_VALUE, false));
                int after = number(API.getEnergyStored.invoke(copy.getItem(), copy));
                if (received <= 0 || after <= current || after > max) return null;
                current = after;
            }
            return current >= max ? copy : null;
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
        final Method receiveEnergy;
        final Method extractEnergy;
        final Method getEnergyStored;
        final Method getMaxEnergyStored;

        private Api(Class<?> energyItemClass, Method receiveEnergy, Method extractEnergy, Method getEnergyStored,
            Method getMaxEnergyStored) {
            this.energyItemClass = energyItemClass;
            this.receiveEnergy = receiveEnergy;
            this.extractEnergy = extractEnergy;
            this.getEnergyStored = getEnergyStored;
            this.getMaxEnergyStored = getMaxEnergyStored;
        }

        static Api load() {
            try {
                Class<?> energyItemClass = Class.forName("cofh.api.energy.IEnergyContainerItem");
                return new Api(
                    energyItemClass,
                    energyItemClass.getMethod("receiveEnergy", ItemStack.class, int.class, boolean.class),
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
