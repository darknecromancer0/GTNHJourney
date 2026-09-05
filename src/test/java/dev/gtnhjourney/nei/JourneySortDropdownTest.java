package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JourneySortDropdownTest {

    @AfterEach
    void reset() {
        JourneySortState.reset();
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
    }

    @Test
    void groupMenuContainsNoneNativeModTypeAndKind() {
        JourneySortDropdown dropdown = new JourneySortDropdown(JourneySortDropdown.Kind.GROUP);
        List<String> labels = dropdown.optionLabelsForTests();
        assertEquals(5, labels.size());
        assertTrue(labels.get(0).contains("None"));
        assertTrue(labels.get(1).contains("Native NEI family"));
        assertTrue(labels.get(2).contains("Mod"));
        assertTrue(labels.get(3).contains("Type"));
        assertTrue(labels.get(4).contains("Similar kind"));
    }

    @Test
    void favouriteAddedAppearsOnlyInsideFView() {
        JourneySortDropdown dropdown = new JourneySortDropdown(JourneySortDropdown.Kind.ORDER);
        JourneyViewState.setMode(JourneyViewState.Mode.RESEARCHED);
        assertFalse(containsFavouriteAdded(dropdown.optionLabelsForTests()));

        JourneyViewState.setMode(JourneyViewState.Mode.FAVOURITE);
        assertTrue(containsFavouriteAdded(dropdown.optionLabelsForTests()));
    }

    private static boolean containsFavouriteAdded(List<String> labels) {
        for (String label : labels) if (label.contains("Favourite added")) return true;
        return false;
    }
}
