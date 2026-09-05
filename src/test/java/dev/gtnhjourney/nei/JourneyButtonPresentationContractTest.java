package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneyButtonPresentationContractTest {

    @Test
    public void legacyNewestTooltipPointsToIndependentLatestModifier() {
        String inactive = JourneyButtonPresentation.newestTooltip(false);
        String active = JourneyButtonPresentation.newestTooltip(true);

        assertTrue(inactive.contains("independent L sorting modifier"));
        assertTrue(active.contains("Latest activity is active"));
    }

    @Test
    public void scanAndDebugButtonsUseCurrentCompactHeaderThresholds() {
        assertFalse(JourneyButtonPresentation.scanVisible(185));
        assertTrue(JourneyButtonPresentation.scanVisible(186));
        assertFalse(JourneyButtonPresentation.debugToolVisible(203));
        assertTrue(JourneyButtonPresentation.debugToolVisible(204));
    }

    @Test
    public void favouriteTooltipUsesDirectionalAddAndRemoveGestures() {
        String inactive = JourneyButtonPresentation.favouriteTooltip(false);
        String active = JourneyButtonPresentation.favouriteTooltip(true);
        assertTrue(inactive.contains("Alt+LMB from J"));
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
