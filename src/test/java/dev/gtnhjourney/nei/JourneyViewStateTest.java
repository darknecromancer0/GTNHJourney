package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class JourneyViewStateTest {

    @AfterEach
    public void resetMode() {
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
    }

    @Test
    public void researchedButtonTogglesBackToAllOnSecondActivation() {
        assertTrue(JourneyViewState.toggle());
        assertEquals(JourneyViewState.Mode.RESEARCHED, JourneyViewState.mode());

        assertFalse(JourneyViewState.toggle());
        assertEquals(JourneyViewState.Mode.ALL, JourneyViewState.mode());
    }

    @Test
    public void newestButtonTogglesBackToAllOnSecondActivation() {
        assertTrue(JourneyViewState.toggleNewest());
        assertEquals(JourneyViewState.Mode.NEWEST, JourneyViewState.mode());

        assertFalse(JourneyViewState.toggleNewest());
        assertEquals(JourneyViewState.Mode.ALL, JourneyViewState.mode());
    }

    @Test
    public void switchingBetweenJourneyModesStillSelectsTheRequestedMode() {
        JourneyViewState.setMode(JourneyViewState.Mode.NEWEST);
        assertTrue(JourneyViewState.toggle());
        assertEquals(JourneyViewState.Mode.RESEARCHED, JourneyViewState.mode());

        assertTrue(JourneyViewState.toggleNewest());
        assertEquals(JourneyViewState.Mode.NEWEST, JourneyViewState.mode());
    }
}
