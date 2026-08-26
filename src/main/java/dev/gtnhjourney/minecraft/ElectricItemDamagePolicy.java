package dev.gtnhjourney.minecraft;

/** Stable legacy-IC2 durability metadata for an empty rechargeable item. */
public final class ElectricItemDamagePolicy {

    private ElectricItemDamagePolicy() {}

    public static int emptyDamage(int maxDamage, int currentDamage) {
        return maxDamage > 0 ? maxDamage - 1 : Math.max(0, currentDamage);
    }
}
