package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class JourneyRefreshDecisionTest {

    @Test
    public void researchChangeInsideJourneyRefreshesOnlyJourneyPanel() {
        assertEquals(
            JourneyRefreshDecision.Action.PANEL_REFRESH,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.RESEARCHED, true, false, false));
        assertEquals(
            JourneyRefreshDecision.Action.PANEL_REFRESH,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.FAVOURITE, true, false, false));
    }

    @Test
    public void nativeNeiFilterChangeRefreshesJourneyPanel() {
        assertEquals(
            JourneyRefreshDecision.Action.PANEL_REFRESH,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.RESEARCHED, false, false, true));
        assertEquals(
            JourneyRefreshDecision.Action.PANEL_REFRESH,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.CREATIVE, false, false, true));
    }

    @Test
    public void enteringJourneyRefreshesPanelAndStableJourneyOnlyEnsuresOwnership() {
        assertEquals(
            JourneyRefreshDecision.Action.PANEL_REFRESH,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.RESEARCHED, false, true, false));
        assertEquals(
            JourneyRefreshDecision.Action.PANEL_ENSURE,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.FAVOURITE, false, false, false));
    }

    @Test
    public void allModeUsesOnlyNormalNeiFilterRefreshWhenViewChanges() {
        assertEquals(
            JourneyRefreshDecision.Action.NEI_FILTER_REFRESH,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.ALL, false, true, false));
        assertEquals(
            JourneyRefreshDecision.Action.NONE,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.ALL, false, false, true));
        assertEquals(
            JourneyRefreshDecision.Action.NONE,
            JourneyRefreshDecision.decide(JourneyViewState.Mode.ALL, false, false, false));
    }

    @Test
    public void researchOnlyRefreshDoesNotRequestPageReset() {
        assertFalse(JourneyRefreshDecision.shouldResetPage(true, false, false));
        assertEquals(true, JourneyRefreshDecision.shouldResetPage(false, true, false));
        assertEquals(true, JourneyRefreshDecision.shouldResetPage(false, false, true));
    }

    @Test
    public void refreshActionsContainNoFullNeiReloadPath() {
        assertFalse(
            Arrays.stream(JourneyRefreshDecision.Action.values())
                .anyMatch(action -> action.name().contains("FULL") || action.name().contains("LOAD")));
    }
}
