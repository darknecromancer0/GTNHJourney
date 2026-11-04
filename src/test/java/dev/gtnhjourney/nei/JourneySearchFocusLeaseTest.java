package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JourneySearchFocusLeaseTest {

    @Test
    void ctrlBackspaceRetainsFocusAcrossDelayedRebuilds() {
        Object gui = new Object();
        JourneySearchFocusLease lease = new JourneySearchFocusLease();
        assertFalse(lease.shouldRestore(gui, true, false));
        assertTrue(lease.onKeyDown(gui, true, true, true));
        assertFalse(lease.shouldRestore(gui, true, true));
        assertTrue(lease.shouldRestore(gui, true, false));
        assertTrue(lease.shouldRestore(gui, true, false));
        assertFalse(lease.shouldRestore(gui, false, false));
        assertTrue(lease.shouldRestore(gui, true, false));
    }

    @Test
    void typingAfterClearRestoresFocusBeforeReleasingLease() {
        Object gui = new Object();
        JourneySearchFocusLease lease = new JourneySearchFocusLease();
        assertTrue(lease.onKeyDown(gui, true, true, true));
        assertTrue(lease.shouldRestore(gui, true, false));
        assertFalse(lease.onKeyDown(gui, false, true, true));
        assertFalse(lease.shouldRestore(gui, true, false));
    }

    @Test
    void mouseAndGuiChangeMustNotStealFocusBack() {
        Object gui = new Object();
        JourneySearchFocusLease lease = new JourneySearchFocusLease();
        lease.arm(gui);
        assertFalse(lease.shouldRestore(new Object(), true, false));
        assertFalse(lease.activeFor(gui));
        lease.arm(gui);
        lease.clear();
        assertFalse(lease.shouldRestore(gui, true, false));
    }

    @Test
    void onlyOriginallyFocusedSearchStartsTheLease() {
        Object gui = new Object();
        JourneySearchFocusLease lease = new JourneySearchFocusLease();
        assertFalse(lease.onKeyDown(gui, true, true, false));
        assertFalse(lease.onKeyDown(gui, true, false, true));
        assertFalse(lease.onKeyDown(null, true, true, true));
        assertFalse(lease.activeFor(gui));
    }
}
