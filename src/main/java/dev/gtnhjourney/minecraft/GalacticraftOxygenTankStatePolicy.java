package dev.gtnhjourney.minecraft;

/** Canonical Journey identity for Galacticraft oxygen tanks. */
public final class GalacticraftOxygenTankStatePolicy {

    static final String LIGHT_TANK = "GalacticraftCore:item.oxygenTankLightFull";
    static final String MEDIUM_TANK = "GalacticraftCore:item.oxygenTankMedFull";
    static final String HEAVY_TANK = "GalacticraftCore:item.oxygenTankHeavyFull";
    static final int FULL_META = 0;

    private GalacticraftOxygenTankStatePolicy() {}

    public static boolean matches(String itemId) {
        return emptyMeta(itemId) >= 0;
    }

    /** EMPTY stays at max damage; any positive oxygen amount collapses to the FULL endpoint. */
    public static int canonicalMeta(String itemId, int meta) {
        int emptyMeta = emptyMeta(itemId);
        if (emptyMeta < 0) return meta;
        return meta >= emptyMeta ? emptyMeta : FULL_META;
    }

    /**
     * Journey <= 1.1.18 collapsed crafted empty tanks through generic durability handling and persisted meta 0. Treat
     * that legacy ambiguous endpoint as EMPTY. A genuinely oxygenated tank observed after migration re-adds FULL.
     */
    public static int migratePersistedMeta(String itemId, int meta) {
        int emptyMeta = emptyMeta(itemId);
        if (emptyMeta < 0) return meta;
        if (meta == FULL_META) return emptyMeta;
        return canonicalMeta(itemId, meta);
    }

    static int emptyMeta(String itemId) {
        if (LIGHT_TANK.equals(itemId)) return 900;
        if (MEDIUM_TANK.equals(itemId)) return 1800;
        if (HEAVY_TANK.equals(itemId)) return 2700;
        return -1;
    }
}
