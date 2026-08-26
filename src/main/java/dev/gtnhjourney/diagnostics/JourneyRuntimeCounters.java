package dev.gtnhjourney.diagnostics;

import java.util.concurrent.atomic.AtomicLong;

/** Process/session-only counters for live Journey diagnostics. */
public final class JourneyRuntimeCounters {

    private static final AtomicLong panelIncrementalUpdates = new AtomicLong();
    private static final AtomicLong panelAuthoritativeStacks = new AtomicLong();
    private static final AtomicLong panelSemanticStacks = new AtomicLong();
    private static final AtomicLong panelVisibleStacks = new AtomicLong();
    private static final AtomicLong fullNeiReloadRequests = new AtomicLong();
    private static final AtomicLong presentationFailures = new AtomicLong();
    private static final AtomicLong creativeUnsafeFlaskVariantsRemoved = new AtomicLong();
    private static final AtomicLong unlockNotifications = new AtomicLong();
    private static final AtomicLong furnaceOutputObservations = new AtomicLong();
    private static final AtomicLong furnaceOutputUnlocks = new AtomicLong();
    private static final AtomicLong debugResearchScans = new AtomicLong();
    private static final AtomicLong debugResearchPositionsVisited = new AtomicLong();
    private static final AtomicLong debugResearchInventoriesVisited = new AtomicLong();
    private static final AtomicLong debugResearchUniqueCandidates = new AtomicLong();
    private static final AtomicLong debugResearchNewStates = new AtomicLong();

    private JourneyRuntimeCounters() {}

    public static void panelIncrementalUpdate() {
        panelIncrementalUpdates.incrementAndGet();
    }

    /** Last direct-panel publication pipeline: client mirror -> semantic keys -> search-visible entries. */
    public static void panelPublication(int authoritativeStacks, int semanticStacks, int visibleStacks) {
        panelAuthoritativeStacks.set(Math.max(0, authoritativeStacks));
        panelSemanticStacks.set(Math.max(0, semanticStacks));
        panelVisibleStacks.set(Math.max(0, visibleStacks));
    }

    public static void fullNeiReloadRequest() {
        fullNeiReloadRequests.incrementAndGet();
    }

    public static void presentationFailure() {
        presentationFailures.incrementAndGet();
    }

    public static void creativeUnsafeFlaskVariantsRemoved(int count) {
        creativeUnsafeFlaskVariantsRemoved.addAndGet(Math.max(0, count));
    }

    public static void unlockNotification() {
        unlockNotifications.incrementAndGet();
    }

    public static void furnaceOutputObservation() {
        furnaceOutputObservations.incrementAndGet();
    }

    public static void furnaceOutputUnlock() {
        furnaceOutputUnlocks.incrementAndGet();
    }

    public static void debugResearchScan(int positionsVisited, int inventoriesVisited, int uniqueCandidates, int newStates) {
        debugResearchScans.incrementAndGet();
        debugResearchPositionsVisited.addAndGet(Math.max(0, positionsVisited));
        debugResearchInventoriesVisited.addAndGet(Math.max(0, inventoriesVisited));
        debugResearchUniqueCandidates.addAndGet(Math.max(0, uniqueCandidates));
        debugResearchNewStates.addAndGet(Math.max(0, newStates));
    }

    public static Snapshot snapshot() {
        return new Snapshot(
            panelIncrementalUpdates.get(),
            panelAuthoritativeStacks.get(),
            panelSemanticStacks.get(),
            panelVisibleStacks.get(),
            fullNeiReloadRequests.get(),
            presentationFailures.get(),
            creativeUnsafeFlaskVariantsRemoved.get(),
            unlockNotifications.get(),
            furnaceOutputObservations.get(),
            furnaceOutputUnlocks.get(),
            debugResearchScans.get(),
            debugResearchPositionsVisited.get(),
            debugResearchInventoriesVisited.get(),
            debugResearchUniqueCandidates.get(),
            debugResearchNewStates.get());
    }

    public static void reset() {
        panelIncrementalUpdates.set(0L);
        panelAuthoritativeStacks.set(0L);
        panelSemanticStacks.set(0L);
        panelVisibleStacks.set(0L);
        fullNeiReloadRequests.set(0L);
        presentationFailures.set(0L);
        creativeUnsafeFlaskVariantsRemoved.set(0L);
        unlockNotifications.set(0L);
        furnaceOutputObservations.set(0L);
        furnaceOutputUnlocks.set(0L);
        debugResearchScans.set(0L);
        debugResearchPositionsVisited.set(0L);
        debugResearchInventoriesVisited.set(0L);
        debugResearchUniqueCandidates.set(0L);
        debugResearchNewStates.set(0L);
    }

    public static final class Snapshot {

        private final long panelIncrementalUpdates;
        private final long panelAuthoritativeStacks;
        private final long panelSemanticStacks;
        private final long panelVisibleStacks;
        private final long fullNeiReloadRequests;
        private final long presentationFailures;
        private final long creativeUnsafeFlaskVariantsRemoved;
        private final long unlockNotifications;
        private final long furnaceOutputObservations;
        private final long furnaceOutputUnlocks;
        private final long debugResearchScans;
        private final long debugResearchPositionsVisited;
        private final long debugResearchInventoriesVisited;
        private final long debugResearchUniqueCandidates;
        private final long debugResearchNewStates;

        private Snapshot(
            long panelIncrementalUpdates,
            long panelAuthoritativeStacks,
            long panelSemanticStacks,
            long panelVisibleStacks,
            long fullNeiReloadRequests,
            long presentationFailures,
            long creativeUnsafeFlaskVariantsRemoved,
            long unlockNotifications,
            long furnaceOutputObservations,
            long furnaceOutputUnlocks,
            long debugResearchScans,
            long debugResearchPositionsVisited,
            long debugResearchInventoriesVisited,
            long debugResearchUniqueCandidates,
            long debugResearchNewStates) {
            this.panelIncrementalUpdates = panelIncrementalUpdates;
            this.panelAuthoritativeStacks = panelAuthoritativeStacks;
            this.panelSemanticStacks = panelSemanticStacks;
            this.panelVisibleStacks = panelVisibleStacks;
            this.fullNeiReloadRequests = fullNeiReloadRequests;
            this.presentationFailures = presentationFailures;
            this.creativeUnsafeFlaskVariantsRemoved = creativeUnsafeFlaskVariantsRemoved;
            this.unlockNotifications = unlockNotifications;
            this.furnaceOutputObservations = furnaceOutputObservations;
            this.furnaceOutputUnlocks = furnaceOutputUnlocks;
            this.debugResearchScans = debugResearchScans;
            this.debugResearchPositionsVisited = debugResearchPositionsVisited;
            this.debugResearchInventoriesVisited = debugResearchInventoriesVisited;
            this.debugResearchUniqueCandidates = debugResearchUniqueCandidates;
            this.debugResearchNewStates = debugResearchNewStates;
        }

        public long getPanelIncrementalUpdates() {
            return panelIncrementalUpdates;
        }

        public long getPanelAuthoritativeStacks() {
            return panelAuthoritativeStacks;
        }

        public long getPanelSemanticStacks() {
            return panelSemanticStacks;
        }

        public long getPanelVisibleStacks() {
            return panelVisibleStacks;
        }

        public long getFullNeiReloadRequests() {
            return fullNeiReloadRequests;
        }

        public long getPresentationFailures() {
            return presentationFailures;
        }

        public long getCreativeUnsafeFlaskVariantsRemoved() {
            return creativeUnsafeFlaskVariantsRemoved;
        }

        public long getUnlockNotifications() {
            return unlockNotifications;
        }

        public long getFurnaceOutputObservations() {
            return furnaceOutputObservations;
        }

        public long getFurnaceOutputUnlocks() {
            return furnaceOutputUnlocks;
        }

        public long getDebugResearchScans() {
            return debugResearchScans;
        }

        public long getDebugResearchPositionsVisited() {
            return debugResearchPositionsVisited;
        }

        public long getDebugResearchInventoriesVisited() {
            return debugResearchInventoriesVisited;
        }

        public long getDebugResearchUniqueCandidates() {
            return debugResearchUniqueCandidates;
        }

        public long getDebugResearchNewStates() {
            return debugResearchNewStates;
        }
    }
}
