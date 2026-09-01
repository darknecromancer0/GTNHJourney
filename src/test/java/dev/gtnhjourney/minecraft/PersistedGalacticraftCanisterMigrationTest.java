package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class PersistedGalacticraftCanisterMigrationTest {

    private static final String OIL = "GalacticraftCore:item.oilCanisterPartial";
    private static final String FUEL = "GalacticraftCore:item.fuelCanisterPartial";

    @Test
    public void legacyZeroFromJourney117IsDroppedFailClosed() {
        assertNull(PersistedResearchEntryResolver.resolve(OIL, 0, "", null));
        assertNull(PersistedResearchEntryResolver.resolve(FUEL, 0, "", null));
    }

    @Test
    public void emptyOilCanisterRemainsEmpty() {
        ResearchKey key = PersistedResearchEntryResolver.resolve(OIL, 1001, "", null);
        assertNotNull(key);
        assertEquals(OIL, key.getItemId());
        assertEquals(1001, key.getMeta());
    }

    @Test
    public void partialOilAndFuelCanistersCollapseToFullEndpoint() {
        assertKey(PersistedResearchEntryResolver.resolve(OIL, 500, "", null), OIL, 1);
        assertKey(PersistedResearchEntryResolver.resolve(FUEL, 500, "", null), FUEL, 1);
    }

    @Test
    public void emptyFuelCanisterCanonicalizesToSharedEmptyOilCanisterIdentity() {
        assertKey(PersistedResearchEntryResolver.resolve(FUEL, 1001, "", null), OIL, 1001);
    }

    private static void assertKey(ResearchKey key, String itemId, int meta) {
        assertNotNull(key);
        assertEquals(itemId, key.getItemId());
        assertEquals(meta, key.getMeta());
    }
}
