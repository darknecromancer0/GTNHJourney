package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/** Regression contract for Thaumcraft aura-node world research without a hard Thaumcraft class dependency. */
public class ThaumcraftNodeResearchFallbackTest {

    @Test
    public void onlyThaumcraftBlockAiryAuraNodeUsesTheKnownRegistryFallback() throws Exception {
        Class<?> policy = Class.forName("dev.gtnhjourney.debug.KnownPlacedBlockResearchFallback");
        Method supports = policy.getDeclaredMethod("supports", String.class, int.class);
        supports.setAccessible(true);

        assertTrue(((Boolean) supports.invoke(null, "Thaumcraft:blockAiry", Integer.valueOf(0))).booleanValue());
        assertFalse(((Boolean) supports.invoke(null, "Thaumcraft:blockAiry", Integer.valueOf(1))).booleanValue());
        assertFalse(((Boolean) supports.invoke(null, "minecraft:air", Integer.valueOf(0))).booleanValue());
    }
}
