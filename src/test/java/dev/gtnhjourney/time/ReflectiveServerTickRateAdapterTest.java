package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ReflectiveServerTickRateAdapterTest {

    @Test
    public void appliesMultiplierThroughObservedServerLoopHook() {
        FakeHook target = new FakeHook();
        ReflectiveServerTickRateAdapter adapter = new ReflectiveServerTickRateAdapter(fixed(target));

        assertTrue(adapter.isSupported());
        assertTrue(adapter.applyMultiplier(4));
        assertTrue(target.multiplier == 4);

        adapter.reset();
        assertTrue(target.multiplier == 1);
    }

    @Test
    public void missingOrUnobservedHookFailsClosed() {
        ReflectiveServerTickRateAdapter missing = new ReflectiveServerTickRateAdapter(fixed(new Object()));
        assertFalse(missing.isSupported());
        assertFalse(missing.applyMultiplier(8));

        FakeHook unobservedTarget = new FakeHook();
        unobservedTarget.observed = false;
        ReflectiveServerTickRateAdapter unobserved = new ReflectiveServerTickRateAdapter(fixed(unobservedTarget));
        assertFalse(unobserved.isSupported());
        assertFalse(unobserved.applyMultiplier(2));
        assertTrue(unobservedTarget.multiplier == 1);
    }

    @Test
    public void thrownHookFailureReturnsFalseInsteadOfEscaping() {
        ReflectiveServerTickRateAdapter adapter = new ReflectiveServerTickRateAdapter(fixed(new ThrowingHook()));
        assertFalse(adapter.isSupported());
        assertFalse(adapter.applyMultiplier(2));
        adapter.reset();
    }

    private static ReflectiveServerTickRateAdapter.TargetProvider fixed(final Object target) {
        return new ReflectiveServerTickRateAdapter.TargetProvider() {
            @Override
            public Object currentServer() {
                return target;
            }
        };
    }

    public static class FakeHook {
        boolean observed = true;
        int multiplier = 1;

        public boolean gtnhjourney$isSpeedHookAvailable() {
            return observed;
        }

        public boolean gtnhjourney$setSpeedMultiplier(int value) {
            if (!observed) return false;
            multiplier = value;
            return true;
        }

        public void gtnhjourney$resetSpeedMultiplier() {
            multiplier = 1;
        }
    }

    public static final class ThrowingHook extends FakeHook {
        @Override
        public boolean gtnhjourney$isSpeedHookAvailable() {
            throw new IllegalStateException("broken hook");
        }

        @Override
        public boolean gtnhjourney$setSpeedMultiplier(int value) {
            throw new IllegalStateException("broken hook");
        }

        @Override
        public void gtnhjourney$resetSpeedMultiplier() {
            throw new IllegalStateException("broken hook");
        }
    }
}
