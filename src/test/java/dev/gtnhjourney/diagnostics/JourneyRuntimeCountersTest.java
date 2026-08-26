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
        JourneyRuntimeCounters.panelPublication(59, 56, 54);
        JourneyRuntimeCounters.fullNeiReloadRequest();
        JourneyRuntimeCounters.unlockNotification();
        JourneyRuntimeCounters.furnaceOutputObservation();
        JourneyRuntimeCounters.furnaceOutputUnlock();
        JourneyRuntimeCounters.debugResearchScan(4096, 14, 63, 21);
        JourneyRuntimeCounters.debugResearchScan(1, 1, 2, 0);

        JourneyRuntimeCounters.Snapshot snapshot = JourneyRuntimeCounters.snapshot();
        assertEquals(1L, snapshot.getPanelIncrementalUpdates());
        assertEquals(59L, snapshot.getPanelAuthoritativeStacks());
        assertEquals(56L, snapshot.getPanelSemanticStacks());
        assertEquals(54L, snapshot.getPanelVisibleStacks());
        assertEquals(1L, snapshot.getFullNeiReloadRequests());
        assertEquals(1L, snapshot.getUnlockNotifications());
        assertEquals(1L, snapshot.getFurnaceOutputObservations());
        assertEquals(1L, snapshot.getFurnaceOutputUnlocks());
        assertEquals(2L, snapshot.getDebugResearchScans());
        assertEquals(4097L, snapshot.getDebugResearchPositionsVisited());
        assertEquals(15L, snapshot.getDebugResearchInventoriesVisited());
        assertEquals(65L, snapshot.getDebugResearchUniqueCandidates());
        assertEquals(21L, snapshot.getDebugResearchNewStates());
    }

    @Test
    public void resetClearsAllCounters() {
        JourneyRuntimeCounters.panelIncrementalUpdate();
        JourneyRuntimeCounters.panelPublication(7, 6, 5);
        JourneyRuntimeCounters.furnaceOutputObservation();
        JourneyRuntimeCounters.debugResearchScan(4096, 3, 50, 7);
        JourneyRuntimeCounters.reset();

        JourneyRuntimeCounters.Snapshot snapshot = JourneyRuntimeCounters.snapshot();
        assertEquals(0L, snapshot.getPanelIncrementalUpdates());
        assertEquals(0L, snapshot.getPanelAuthoritativeStacks());
        assertEquals(0L, snapshot.getPanelSemanticStacks());
        assertEquals(0L, snapshot.getPanelVisibleStacks());
        assertEquals(0L, snapshot.getFullNeiReloadRequests());
        assertEquals(0L, snapshot.getUnlockNotifications());
        assertEquals(0L, snapshot.getFurnaceOutputObservations());
        assertEquals(0L, snapshot.getFurnaceOutputUnlocks());
        assertEquals(0L, snapshot.getDebugResearchScans());
        assertEquals(0L, snapshot.getDebugResearchPositionsVisited());
        assertEquals(0L, snapshot.getDebugResearchInventoriesVisited());
        assertEquals(0L, snapshot.getDebugResearchUniqueCandidates());
        assertEquals(0L, snapshot.getDebugResearchNewStates());
    }
}
