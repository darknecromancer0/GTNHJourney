package dev.gtnhjourney.minecraft;

/** Canonicalizes registry-specific invalid/alias metadata proven to resolve to the same visible obtainable item. */
public final class KnownMetadataAliasPolicy {

    private static final String BOP_HIVE = "BiomesOPlenty:hive";

    private KnownMetadataAliasPolicy() {}

    public static int canonicalMeta(String itemId, int meta) {
        // Biomes O' Plenty 1.7.10 only defines hive metas 0..3. ItemBlockHive and BlockHive both fall invalid metas
        // back to the honeycomb (meta 0) name/icon. Live GTNH data contained meta 9 as a duplicate Honeycomb Block.
        if (BOP_HIVE.equals(itemId) && (meta < 0 || meta > 3)) return 0;
        return meta;
    }
}
