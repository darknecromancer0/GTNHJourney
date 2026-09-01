package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class VanillaMetadataPolicyTest {

    @Test
    public void dirtKeepsPodzolButCollapsesDefaultAndGarbageMetadata() throws Exception {
        Class<?> policy = Class.forName("dev.gtnhjourney.minecraft.VanillaMetadataPolicy");
        Method canonicalMeta = policy.getDeclaredMethod("canonicalMeta", String.class, int.class);

        assertEquals(0, ((Integer) canonicalMeta.invoke(null, "minecraft:dirt", 0)).intValue());
        assertEquals(0, ((Integer) canonicalMeta.invoke(null, "minecraft:dirt", 1)).intValue());
        assertEquals(2, ((Integer) canonicalMeta.invoke(null, "minecraft:dirt", 2)).intValue());
        assertEquals(0, ((Integer) canonicalMeta.invoke(null, "minecraft:dirt", 7)).intValue());
        assertEquals(0, ((Integer) canonicalMeta.invoke(null, "minecraft:dirt", 49)).intValue());
    }

    @Test
    public void unrelatedMetadataRemainsExact() throws Exception {
        Class<?> policy = Class.forName("dev.gtnhjourney.minecraft.VanillaMetadataPolicy");
        Method canonicalMeta = policy.getDeclaredMethod("canonicalMeta", String.class, int.class);

        assertEquals(49, ((Integer) canonicalMeta.invoke(null, "example:modded_item", 49)).intValue());
    }
}
