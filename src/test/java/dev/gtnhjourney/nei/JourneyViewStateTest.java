package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class JourneyViewStateTest {

    @AfterEach
    public void resetMode() {
        JourneySortState.reset();
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
    }

    @Test
    public void researchedCompatibilityToggleStillTogglesBackToAll() {
        assertTrue(JourneyViewState.toggle());
        assertEquals(JourneyViewState.Mode.RESEARCHED, JourneyViewState.mode());

        assertFalse(JourneyViewState.toggle());
        assertEquals(JourneyViewState.Mode.ALL, JourneyViewState.mode());
    }

    @Test
    public void legacyNewestToggleMapsToResearchedPlusLatestInsteadOfAContentMode() {
        assertTrue(JourneyViewState.toggleNewest());
        assertEquals(JourneyViewState.Mode.RESEARCHED, JourneyViewState.mode());
        assertTrue(JourneySortState.latest(JourneyViewState.Mode.RESEARCHED));
        assertTrue(JourneyViewState.isNewest());

        assertFalse(JourneyViewState.toggleNewest());
        assertEquals(JourneyViewState.Mode.RESEARCHED, JourneyViewState.mode());
        assertFalse(JourneySortState.latest(JourneyViewState.Mode.RESEARCHED));
        assertFalse(JourneyViewState.isNewest());
    }

    @Test
    public void changingContentViewDoesNotEraseThatViewsRememberedSortSettings() {
        JourneySortState.setGroup(JourneyViewState.Mode.RESEARCHED, JourneyGroupMode.NATIVE);
        JourneySortState.setLatest(JourneyViewState.Mode.RESEARCHED, true);
        JourneyViewState.setMode(JourneyViewState.Mode.FAVOURITE);
        JourneyViewState.setMode(JourneyViewState.Mode.RESEARCHED);

        assertEquals(JourneyGroupMode.NATIVE, JourneySortState.group(JourneyViewState.Mode.RESEARCHED));
        assertTrue(JourneySortState.latest(JourneyViewState.Mode.RESEARCHED));
    }
}
