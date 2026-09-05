package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

public class GalacticraftRocketFuelStatePolicyTest {

    @Test
    public void emptyAndZeroFuelShareOneEmptyState() {
        NBTTagCompound missing = new NBTTagCompound();
        NBTTagCompound zero = new NBTTagCompound();
        zero.setInteger("RocketFuel", 0);

        GalacticraftRocketFuelStatePolicy.normalizeForTests("GalacticraftCore:item.spaceship", missing, 5);
        GalacticraftRocketFuelStatePolicy.normalizeForTests("GalacticraftCore:item.spaceship", zero, 5);

        assertFalse(missing.hasKey("RocketFuel"));
        assertFalse(zero.hasKey("RocketFuel"));
    }

    @Test
    public void anyPositiveTierOneFuelCollapsesToConfiguredFullEndpoint() {
        NBTTagCompound partial = tag(3985);
        NBTTagCompound full = tag(5000);

        GalacticraftRocketFuelStatePolicy.normalizeForTests("GalacticraftCore:item.spaceship", partial, 5);
        GalacticraftRocketFuelStatePolicy.normalizeForTests("GalacticraftCore:item.spaceship", full, 5);

        assertEquals(5000, partial.getInteger("RocketFuel"));
        assertEquals(5000, full.getInteger("RocketFuel"));
        assertEquals(partial.toString(), full.toString());
    }

    @Test
    public void tierTwoAndThreeUseTheirVerifiedBaseTankTimesLiveFuelFactor() {
        NBTTagCompound tierTwo = tag(1);
        NBTTagCompound tierThree = tag(7499);

        GalacticraftRocketFuelStatePolicy.normalizeForTests("GalacticraftMars:item.spaceshipTier2", tierTwo, 5);
        GalacticraftRocketFuelStatePolicy.normalizeForTests("GalacticraftMars:item.itemTier3Rocket", tierThree, 5);

        assertEquals(7500, tierTwo.getInteger("RocketFuel"));
        assertEquals(7500, tierThree.getInteger("RocketFuel"));
    }

    @Test
    public void rocketMetadataIsOutsideThisPolicyAndOtherNbtSurvives() {
        NBTTagCompound tag = tag(10);
        tag.setString("JourneySentinel", "keep");

        GalacticraftRocketFuelStatePolicy.normalizeForTests("GalacticraftCore:item.spaceship", tag, 3);

        assertEquals(3000, tag.getInteger("RocketFuel"));
        assertEquals("keep", tag.getString("JourneySentinel"));
    }

    @Test
    public void unrelatedRocketFuelTagRemainsExactFailClosed() {
        NBTTagCompound unrelated = tag(3985);

        GalacticraftRocketFuelStatePolicy.normalizeForTests("example:customRocket", unrelated, 5);

        assertTrue(unrelated.hasKey("RocketFuel"));
        assertEquals(3985, unrelated.getInteger("RocketFuel"));
    }

    private static NBTTagCompound tag(int fuel) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("RocketFuel", fuel);
        return tag;
    }
}
