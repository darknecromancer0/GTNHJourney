package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneyButtonPresentationContractTest {

    @Test
    public void newestTooltipDescribesFullResearchSetAndActivityOrdering() {
        String inactive = JourneyButtonPresentation.newestTooltip(false);
        String active = JourneyButtonPresentation.newestTooltip(true);

        assertTrue(inactive.contains("all researched"));
        assertTrue(inactive.contains("recent Journey activity"));
        assertTrue(active.contains("all researched"));
        assertTrue(active.contains("recent Journey activity"));
    }

    @Test
    public void scanAndDebugButtonsOnlyAppearWhenHeaderHasRoom() {
        assertFalse(JourneyButtonPresentation.scanVisible(167));
        assertTrue(JourneyButtonPresentation.scanVisible(168));
        assertFalse(JourneyButtonPresentation.debugToolVisible(185));
        assertTrue(JourneyButtonPresentation.debugToolVisible(186));
    }

    @Test
    public void scanTooltipExplainsDeepRecoveryAndRefresh() {
        String tooltip = JourneyButtonPresentation.scanTooltip();
        assertTrue(tooltip.contains("inventory"));
        assertTrue(tooltip.contains("container"));
        assertTrue(tooltip.contains("refresh"));
    }

    @Test
    public void debugToolTooltipExplainsPermissionBoundary() {
        String tooltip = JourneyButtonPresentation.debugToolTooltip();
        assertTrue(tooltip.contains("Debug Researcher Tool"));
        assertTrue(tooltip.contains("owner/operator"));
    }
}
