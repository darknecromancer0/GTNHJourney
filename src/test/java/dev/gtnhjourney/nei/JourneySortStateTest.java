package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class JourneySortStateTest {

    @AfterEach
    public void reset() {
        JourneySortState.reset();
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
    }

    @Test
    public void newestIsNoLongerAContentView() {
        assertFalse(Arrays.asList(JourneyViewState.Mode.values()).toString().contains("NEWEST"));
    }

    @Test
    public void eachViewRemembersIndependentGroupOrderAndLatest() {
        JourneySortState.setGroup(JourneyViewState.Mode.ALL, JourneyGroupMode.MOD);
        JourneySortState.setOrder(JourneyViewState.Mode.ALL, JourneyOrderMode.ALPHABETICAL);

        JourneySortState.setGroup(JourneyViewState.Mode.RESEARCHED, JourneyGroupMode.NATIVE);
        JourneySortState.setOrder(JourneyViewState.Mode.RESEARCHED, JourneyOrderMode.UNLOCK);
        JourneySortState.setLatest(JourneyViewState.Mode.RESEARCHED, true);

        JourneySortState.setGroup(JourneyViewState.Mode.FAVOURITE, JourneyGroupMode.NONE);
        JourneySortState.setOrder(JourneyViewState.Mode.FAVOURITE, JourneyOrderMode.FAVOURITE_ADDED);

        JourneySortState.setGroup(JourneyViewState.Mode.CREATIVE, JourneyGroupMode.MOD);
        JourneySortState.setLatest(JourneyViewState.Mode.CREATIVE, true);

        JourneySortState.setGroup(JourneyViewState.Mode.DELETE, JourneyGroupMode.KIND);
        JourneySortState.setOrder(JourneyViewState.Mode.DELETE, JourneyOrderMode.ALPHABETICAL);

        assertEquals(JourneyGroupMode.MOD, JourneySortState.group(JourneyViewState.Mode.ALL));
        assertEquals(JourneyOrderMode.ALPHABETICAL, JourneySortState.order(JourneyViewState.Mode.ALL));
        assertEquals(JourneyGroupMode.NATIVE, JourneySortState.group(JourneyViewState.Mode.RESEARCHED));
        assertEquals(JourneyOrderMode.UNLOCK, JourneySortState.order(JourneyViewState.Mode.RESEARCHED));
        assertTrue(JourneySortState.latest(JourneyViewState.Mode.RESEARCHED));
        assertEquals(JourneyOrderMode.FAVOURITE_ADDED, JourneySortState.order(JourneyViewState.Mode.FAVOURITE));
        assertEquals(JourneyGroupMode.MOD, JourneySortState.group(JourneyViewState.Mode.CREATIVE));
        assertTrue(JourneySortState.latest(JourneyViewState.Mode.CREATIVE));
        assertEquals(JourneyGroupMode.KIND, JourneySortState.group(JourneyViewState.Mode.DELETE));
        assertEquals(JourneyOrderMode.ALPHABETICAL, JourneySortState.order(JourneyViewState.Mode.DELETE));
    }

    @Test
    public void favouriteAddedIsRejectedOutsideFavouriteView() {
        JourneySortState.setOrder(JourneyViewState.Mode.ALL, JourneyOrderMode.FAVOURITE_ADDED);
        JourneySortState.setOrder(JourneyViewState.Mode.RESEARCHED, JourneyOrderMode.FAVOURITE_ADDED);
        JourneySortState.setOrder(JourneyViewState.Mode.CREATIVE, JourneyOrderMode.FAVOURITE_ADDED);
        assertEquals(JourneyOrderMode.NONE, JourneySortState.order(JourneyViewState.Mode.ALL));
        assertEquals(JourneyOrderMode.NONE, JourneySortState.order(JourneyViewState.Mode.RESEARCHED));
        assertEquals(JourneyOrderMode.NONE, JourneySortState.order(JourneyViewState.Mode.CREATIVE));
    }

    @Test
    public void groupAndOrderCanBeDisabledLeavingPureLatest() {
        JourneySortState.setGroup(JourneyViewState.Mode.RESEARCHED, JourneyGroupMode.NATIVE);
        JourneySortState.setOrder(JourneyViewState.Mode.RESEARCHED, JourneyOrderMode.UNLOCK);
        JourneySortState.setLatest(JourneyViewState.Mode.RESEARCHED, true);
        JourneySortState.setGroup(JourneyViewState.Mode.RESEARCHED, JourneyGroupMode.NONE);
        JourneySortState.setOrder(JourneyViewState.Mode.RESEARCHED, JourneyOrderMode.NONE);
        assertEquals(JourneyGroupMode.NONE, JourneySortState.group(JourneyViewState.Mode.RESEARCHED));
        assertEquals(JourneyOrderMode.NONE, JourneySortState.order(JourneyViewState.Mode.RESEARCHED));
        assertTrue(JourneySortState.latest(JourneyViewState.Mode.RESEARCHED));
    }

    @Test
    public void nativeNeiHasZeroTransformFastPath() {
        assertFalse(JourneySortState.hasTransform(JourneyViewState.Mode.ALL));
        JourneySortState.setGroup(JourneyViewState.Mode.ALL, JourneyGroupMode.NATIVE);
        assertTrue(JourneySortState.hasTransform(JourneyViewState.Mode.ALL));
        JourneySortState.setGroup(JourneyViewState.Mode.ALL, JourneyGroupMode.NONE);
        assertFalse(JourneySortState.hasTransform(JourneyViewState.Mode.ALL));
    }
}
