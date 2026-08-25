package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneyRefreshFlowTest {

    @Test
    public void journeyModeFilterNeverRecursivelyFiltersControllerOwnedPanel() {
        assertTrue(JourneyItemFilterModePolicy.allowThrough(JourneyViewState.Mode.ALL));
        assertTrue(JourneyItemFilterModePolicy.allowThrough(JourneyViewState.Mode.RESEARCHED));
        assertTrue(JourneyItemFilterModePolicy.allowThrough(JourneyViewState.Mode.NEWEST));
    }

    @Test
    public void refreshFlowUsesOnlyPanelOwnershipAndNormalNeiFilterRefresh() {
        assertEquals(
            JourneyRefreshDecision.Action.PANEL_REFRESH,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.RESEARCHED, true, false));
        assertEquals(
            JourneyRefreshDecision.Action.PANEL_ENSURE,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.NEWEST, false, false));
        assertEquals(
            JourneyRefreshDecision.Action.NEI_FILTER_REFRESH,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.ALL, false, true));
    }
}
