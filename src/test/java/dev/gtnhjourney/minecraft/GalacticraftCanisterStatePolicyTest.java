package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GalacticraftCanisterStatePolicyTest {

    private static final String OIL = "GalacticraftCore:item.oilCanisterPartial";
    private static final String FUEL = "GalacticraftCore:item.fuelCanisterPartial";

    @Test
    public void emptyCanisterKeepsGalacticraftEmptyMetadata() {
        assertEquals(1001, GalacticraftCanisterStatePolicy.canonicalMeta(OIL, 1001));
    }

    @Test
    public void anyPositiveFluidAmountCollapsesToFullEndpoint() {
        assertEquals(1, GalacticraftCanisterStatePolicy.canonicalMeta(OIL, 1000));
        assertEquals(1, GalacticraftCanisterStatePolicy.canonicalMeta(OIL, 500));
        assertEquals(1, GalacticraftCanisterStatePolicy.canonicalMeta(OIL, 1));
        assertEquals(1, GalacticraftCanisterStatePolicy.canonicalMeta(FUEL, 1000));
        assertEquals(1, GalacticraftCanisterStatePolicy.canonicalMeta(FUEL, 1));
    }

    @Test
    public void legacyZeroProducedByJourney117IsAmbiguousAndMustBeDropped() {
        assertTrue(GalacticraftCanisterStatePolicy.isLegacyAmbiguousMeta(OIL, 0));
        assertTrue(GalacticraftCanisterStatePolicy.isLegacyAmbiguousMeta(FUEL, 0));
        assertFalse(GalacticraftCanisterStatePolicy.isLegacyAmbiguousMeta(OIL, 1));
        assertFalse(GalacticraftCanisterStatePolicy.isLegacyAmbiguousMeta(OIL, 1001));
    }

    @Test
    public void unrelatedItemsRemainExact() {
        assertEquals(723, GalacticraftCanisterStatePolicy.canonicalMeta("test:damageable", 723));
        assertFalse(GalacticraftCanisterStatePolicy.isLegacyAmbiguousMeta("test:damageable", 0));
    }
}
