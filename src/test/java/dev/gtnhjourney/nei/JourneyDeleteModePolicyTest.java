package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class JourneyDeleteModePolicyTest {

    @AfterEach
    public void resetMode() {
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
    }

    @Test
    public void deleteButtonTogglesDeleteModeAndBackToAll() {
        assertTrue(JourneyViewState.toggleDelete());
        assertEquals(JourneyViewState.Mode.DELETE, JourneyViewState.mode());
        assertTrue(JourneyViewState.isDelete());

        assertFalse(JourneyViewState.toggleDelete());
        assertEquals(JourneyViewState.Mode.ALL, JourneyViewState.mode());
    }

    @Test
    public void deleteModeListsAllResearchNewestFirst() {
        ResearchKey oldest = new ResearchKey("test:oldest", 0, "");
        ResearchKey newest = new ResearchKey("test:newest", 0, "");
        assertEquals(
            Arrays.asList(newest, oldest),
            JourneyPanelOrder.keysForMode(Arrays.asList(oldest, newest), JourneyViewState.Mode.DELETE, 1));
    }

    @Test
    public void onlyPlainLeftClickDeletesInPre7() {
        assertTrue(JourneyDeleteClickPolicy.shouldDelete(0, false));
        assertFalse(JourneyDeleteClickPolicy.shouldDelete(0, true));
        assertFalse(JourneyDeleteClickPolicy.shouldDelete(1, false));
        assertFalse(JourneyDeleteClickPolicy.shouldDelete(2, false));
    }
}
