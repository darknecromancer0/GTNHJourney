package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class TconTransientCounterPolicyTest {

    @Test
    public void knownIguanaExtraCountersAreTransientButRealModifierKeysAreNot() throws Exception {
        Method classifier = TconToolStatePolicy.class.getDeclaredMethod("isTransientIguanaCounterKey", String.class);
        classifier.setAccessible(true);

        assertTrue((Boolean) classifier.invoke(null, "ExtraRedstone"));
        assertTrue((Boolean) classifier.invoke(null, "ExtraLuckLooting"));
        assertTrue((Boolean) classifier.invoke(null, "ExtraCritical"));
        assertFalse((Boolean) classifier.invoke(null, "Redstone"));
        assertFalse((Boolean) classifier.invoke(null, "Modifiers"));
        assertFalse((Boolean) classifier.invoke(null, "ExtraCustomData"));
    }
}
