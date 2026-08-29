package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneyNeiFilterProviderPolicyTest {

    @Test
    public void visibleSearchFieldRemainsACompatibleJourneyFilter() {
        assertTrue(JourneyNeiFilterProviderPolicy.shouldApply("codechicken.nei.SearchField$GuiSearchField", true, true));
    }

    @Test
    public void hiddenSearchFieldCannotLeakStaleTextIntoJourney() {
        assertFalse(JourneyNeiFilterProviderPolicy.shouldApply("codechicken.nei.SearchField$GuiSearchField", true, false));
    }

    @Test
    public void neiSubsetHiddenItemMaskCannotEraseJourneyResearch() {
        assertFalse(JourneyNeiFilterProviderPolicy.shouldApply("codechicken.nei.SubsetWidget", false, true));
    }

    @Test
    public void unrelatedCompatibleProviderIsStillHonored() {
        assertTrue(JourneyNeiFilterProviderPolicy.shouldApply("example.nei.CompatibleFilterProvider", false, true));
    }
}
