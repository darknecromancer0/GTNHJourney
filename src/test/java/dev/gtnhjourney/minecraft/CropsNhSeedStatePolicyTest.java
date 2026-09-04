package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;

public class CropsNhSeedStatePolicyTest {

    @Test
    public void identityKeepsCropSpeciesButIgnoresMutableStats() {
        NBTTagCompound first = seed("cropsnh:potato", 10, 10, 10, 1);
        NBTTagCompound second = seed("cropsnh:potato", 12, 10, 10, 0);

        assertEquals(
            NbtCanonicalizer.canonicalize(CropsNhSeedStatePolicy.identityTag("cropsnh:genericSeed", first)),
            NbtCanonicalizer.canonicalize(CropsNhSeedStatePolicy.identityTag("cropsnh:genericSeed", second)));
        assertEquals("cropsnh:potato", CropsNhSeedStatePolicy.identityTag("cropsnh:genericSeed", first).getString("crop"));
        assertFalse(CropsNhSeedStatePolicy.identityTag("cropsnh:genericSeed", first).hasKey("ga"));
    }

    @Test
    public void mergeAccumulatesBestValueOfEverySeedStat() {
        NBTTagCompound merged = CropsNhSeedStatePolicy.merge(
            "cropsnh:genericSeed",
            seed("cropsnh:potato", 10, 10, 10, 1),
            seed("cropsnh:potato", 12, 10, 10, 0));
        merged = CropsNhSeedStatePolicy.merge(
            "cropsnh:genericSeed",
            merged,
            seed("cropsnh:potato", 10, 12, 10, 0));

        assertEquals(12, merged.getByte("ga"));
        assertEquals(12, merged.getByte("gr"));
        assertEquals(10, merged.getByte("re"));
        assertEquals(1, merged.getByte("scan"));
    }

    @Test
    public void differentCropSpeciesNeverMerge() {
        assertTrue(
            CropsNhSeedStatePolicy.merge(
                "cropsnh:genericSeed",
                seed("cropsnh:potato", 31, 31, 31, 1),
                seed("cropsnh:onion", 10, 10, 10, 1)) == null);
    }

    private static NBTTagCompound seed(String crop, int gain, int growth, int resistance, int scan) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("crop", crop);
        tag.setByte("ga", (byte) gain);
        tag.setByte("gr", (byte) growth);
        tag.setByte("re", (byte) resistance);
        tag.setByte("scan", (byte) scan);
        return tag;
    }
}
