package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JourneyRecipeBackGuardTest {

    @Test
    void suppressesRecipeBackWhenChordIsStillPhysicallyHeldOnAnotherKeyboardEvent() {
        assertTrue(JourneyRecipeBackGuard.shouldSuppress(true, true, false));
    }

    @Test
    void suppressesHeldBackspaceWhileMainNeiSearchStillOwnsKeyboardFocus() {
        assertTrue(JourneyRecipeBackGuard.shouldSuppress(true, false, true));
    }

    @Test
    void preservesPlainRecipeBackOutsideSearchAndWithoutCtrl() {
        assertFalse(JourneyRecipeBackGuard.shouldSuppress(true, false, false));
    }

    @Test
    void ignoresUnrelatedKeysWhenRecipeBackIsNotPhysicallyDown() {
        assertFalse(JourneyRecipeBackGuard.shouldSuppress(false, true, true));
    }
}
