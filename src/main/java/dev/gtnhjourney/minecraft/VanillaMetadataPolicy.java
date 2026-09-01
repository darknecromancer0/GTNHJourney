package dev.gtnhjourney.minecraft;

/** Canonicalizes only vanilla metadata values proven to represent the same obtainable item state. */
public final class VanillaMetadataPolicy {

    private static final String DIRT_ID = "minecraft:dirt";
    private static final int PODZOL_META = 2;

    private VanillaMetadataPolicy() {}

    public static int canonicalMeta(String itemId, int meta) {
        if (DIRT_ID.equals(itemId)) return meta == PODZOL_META ? PODZOL_META : 0;
        return meta;
    }
}
