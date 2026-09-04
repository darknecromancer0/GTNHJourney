package dev.gtnhjourney.minecraft;

import net.minecraft.nbt.NBTTagCompound;

/** One Journey identity per CropsNH crop species while retaining the best observed seed statistics. */
public final class CropsNhSeedStatePolicy {

    public static final String GENERIC_SEED = "cropsnh:genericSeed";
    private static final String CROP = "crop";
    private static final String GAIN = "ga";
    private static final String GROWTH = "gr";
    private static final String RESISTANCE = "re";
    private static final String SCAN = "scan";

    private CropsNhSeedStatePolicy() {}

    public static NBTTagCompound identityTag(String itemId, NBTTagCompound tag) {
        if (!GENERIC_SEED.equals(itemId) || tag == null || !tag.hasKey(CROP, 8) || tag.getString(CROP).isEmpty()) {
            return tag == null ? null : (NBTTagCompound) tag.copy();
        }
        NBTTagCompound identity = new NBTTagCompound();
        identity.setString(CROP, tag.getString(CROP));
        return identity;
    }

    public static NBTTagCompound merge(String itemId, NBTTagCompound existing, NBTTagCompound observed) {
        if (!GENERIC_SEED.equals(itemId) || existing == null || observed == null) return null;
        String existingCrop = existing.getString(CROP);
        String observedCrop = observed.getString(CROP);
        if (existingCrop.isEmpty() || !existingCrop.equals(observedCrop)) return null;

        NBTTagCompound merged = (NBTTagCompound) existing.copy();
        maxByte(merged, observed, GAIN);
        maxByte(merged, observed, GROWTH);
        maxByte(merged, observed, RESISTANCE);
        maxByte(merged, observed, SCAN);
        return merged;
    }

    private static void maxByte(NBTTagCompound target, NBTTagCompound source, String key) {
        if (!source.hasKey(key, 99)) return;
        int sourceValue = source.getInteger(key);
        int targetValue = target.hasKey(key, 99) ? target.getInteger(key) : Integer.MIN_VALUE;
        if (sourceValue > targetValue) target.setByte(key, (byte) sourceValue);
    }
}
