package dev.gtnhjourney.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class JourneyRuntimeCountersTest {

    @AfterEach
    public void cleanup() {
        JourneyRuntimeCounters.reset();
    }

    @Test
    public void snapshotReportsEveryPre7RuntimeCounter() {
        JourneyRuntimeCounters.reset();
        JourneyRuntimeCounters.panelIncrementalUpdate();
        JourneyRuntimeCounters.fullNeiReloadRequest();
        JourneyRuntimeCounters.unlockNotification();
        JourneyRuntimeCounters.furnaceOutputObservation();
        JourneyRuntimeCounters.furnaceOutputUnlock();

        JourneyRuntimeCounters.Snapshot snapshot = JourneyRuntimeCounters.snapshot();
        assertEquals(1L, snapshot.getPanelIncrementalUpdates());
        assertEquals(1L, snapshot.getFullNeiReloadRequests());
        assertEquals(1L, snapshot.getUnlockNotifications());
        assertEquals(1L, snapshot.getFurnaceOutputObservations());
        assertEquals(1L, snapshot.getFurnaceOutputUnlocks());
    }

    @Test
    public void resetClearsAllCounters() {
        JourneyRuntimeCounters.panelIncrementalUpdate();
        JourneyRuntimeCounters.furnaceOutputObservation();
        JourneyRuntimeCounters.reset();

        JourneyRuntimeCounters.Snapshot snapshot = JourneyRuntimeCounters.snapshot();
        assertEquals(0L, snapshot.getPanelIncrementalUpdates());
        assertEquals(0L, snapshot.getFullNeiReloadRequests());
        assertEquals(0L, snapshot.getUnlockNotifications());
        assertEquals(0L, snapshot.getFurnaceOutputObservations());
        assertEquals(0L, snapshot.getFurnaceOutputUnlocks());
    }
}
