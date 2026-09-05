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
        assertTrue(inactive.contains("Alt+LMB adds to F"));
        assertTrue(active.contains("all researched"));
        assertTrue(active.contains("recent Journey activity"));
        assertTrue(active.contains("Alt+LMB adds to F"));
    }

    @Test
    public void scanAndDebugButtonsOnlyAppearWhenHeaderHasRoomAfterFCDButtons() {
        assertFalse(JourneyButtonPresentation.scanVisible(203));
        assertTrue(JourneyButtonPresentation.scanVisible(204));
        assertFalse(JourneyButtonPresentation.debugToolVisible(221));
        assertTrue(JourneyButtonPresentation.debugToolVisible(222));
    }

    @Test
    public void favouriteTooltipUsesDirectionalAddAndRemoveGestures() {
        String inactive = JourneyButtonPresentation.favouriteTooltip(false);
        String active = JourneyButtonPresentation.favouriteTooltip(true);
        assertTrue(inactive.contains("Alt+LMB from J/N"));
        assertTrue(inactive.contains("Alt+RMB in F"));
        assertTrue(active.contains("Alt+RMB removes from F"));
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
