package dev.gtnhjourney.minecraft;

/** Canonical Journey identity for Galacticraft liquid canisters. */
public final class GalacticraftCanisterStatePolicy {

    static final String OIL_CANISTER = "GalacticraftCore:item.oilCanisterPartial";
    static final String FUEL_CANISTER = "GalacticraftCore:item.fuelCanisterPartial";
    static final int FULL_META = 1;
    static final int EMPTY_META = 1001;

    private GalacticraftCanisterStatePolicy() {}

    public static boolean matches(String itemId) {
        return OIL_CANISTER.equals(itemId) || FUEL_CANISTER.equals(itemId);
    }

    public static String canonicalItemId(String itemId, int meta) {
        if (!matches(itemId)) return itemId;
        return meta >= EMPTY_META ? OIL_CANISTER : itemId;
    }

    public static int canonicalMeta(String itemId, int meta) {
        if (!matches(itemId)) return meta;
        if (meta >= EMPTY_META) return EMPTY_META;
        return FULL_META;
    }

    public static boolean isLegacyAmbiguousMeta(String itemId, int meta) {
        return matches(itemId) && meta == 0;
    }
}
