package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class PersistedGalacticraftRocketMigrationTest {

    @Test
    public void legacyPartialAndFullTierOneRocketResolveToSameEndpointIdentity() {
        ResearchKey partial = PersistedResearchEntryResolver.resolve(
            "GalacticraftCore:item.spaceship",
            3,
            "legacy-partial",
            fuel(3985));
        ResearchKey full = PersistedResearchEntryResolver.resolve(
            "GalacticraftCore:item.spaceship",
            3,
            "legacy-full",
            fuel(5000));

        assertNotNull(partial);
        assertNotNull(full);
        assertEquals(partial, full);
        assertEquals(3, partial.getMeta());
    }

    @Test
    public void zeroFuelAndMissingFuelResolveToSameEmptyIdentity() {
        ResearchKey zero = PersistedResearchEntryResolver.resolve(
            "GalacticraftCore:item.spaceship",
            0,
            "legacy-zero",
            fuel(0));
        ResearchKey missing = PersistedResearchEntryResolver.resolve(
            "GalacticraftCore:item.spaceship",
            0,
            "",
            new NBTTagCompound());

        assertNotNull(zero);
        assertNotNull(missing);
        assertEquals(zero, missing);
    }

    private static NBTTagCompound fuel(int amount) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("RocketFuel", amount);
        return tag;
    }
}
