package dev.gtnhjourney.minecraft;

/** Canonical Journey identity for Galacticraft oxygen tanks in GTNH. */
public final class GalacticraftOxygenTankStatePolicy {

    static final String LIGHT_TANK = "GalacticraftCore:item.oxygenTankLightFull";
    static final String MEDIUM_TANK = "GalacticraftCore:item.oxygenTankMedFull";
    static final String HEAVY_TANK = "GalacticraftCore:item.oxygenTankHeavyFull";
    static final int FULL_META = 0;

    private GalacticraftOxygenTankStatePolicy() {}

    public static boolean matches(String itemId) {
        return emptyMeta(itemId) >= 0;
    }

    /** EMPTY is GTNH's tier-specific zero-percent damage; any positive oxygen amount collapses to FULL. */
    public static int canonicalMeta(String itemId, int meta) {
        int emptyMeta = emptyMeta(itemId);
        if (emptyMeta < 0) return meta;
        return meta >= emptyMeta ? emptyMeta : FULL_META;
    }

    /**
     * Journey 1.1.19 used upstream Galacticraft's 900/1800/2700 empty values, which display as 10% in GTNH because
     * GTNH recipes deliberately use 1000/2000/4000. Migrate only those known bad 1.1.19 endpoints. Meta 0 is already
     * the legitimate FULL endpoint in 1.1.19 and must remain FULL.
     */
    public static int migratePersistedMeta(String itemId, int meta) {
        int emptyMeta = emptyMeta(itemId);
        if (emptyMeta < 0) return meta;
        if (meta == legacy119EmptyMeta(itemId)) return emptyMeta;
        return canonicalMeta(itemId, meta);
    }

    static int emptyMeta(String itemId) {
        if (LIGHT_TANK.equals(itemId)) return 1000;
        if (MEDIUM_TANK.equals(itemId)) return 2000;
        if (HEAVY_TANK.equals(itemId)) return 4000;
        return -1;
    }

    private static int legacy119EmptyMeta(String itemId) {
        if (LIGHT_TANK.equals(itemId)) return 900;
        if (MEDIUM_TANK.equals(itemId)) return 1800;
        if (HEAVY_TANK.equals(itemId)) return 2700;
        return -1;
    }
}
