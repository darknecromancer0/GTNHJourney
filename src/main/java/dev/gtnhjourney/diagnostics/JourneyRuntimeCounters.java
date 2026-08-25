package dev.gtnhjourney.diagnostics;

import java.util.concurrent.atomic.AtomicLong;

/** Process/session-only counters for live pre7 diagnostics. */
public final class JourneyRuntimeCounters {

    private static final AtomicLong panelIncrementalUpdates = new AtomicLong();
    private static final AtomicLong fullNeiReloadRequests = new AtomicLong();
    private static final AtomicLong unlockNotifications = new AtomicLong();
    private static final AtomicLong furnaceOutputObservations = new AtomicLong();
    private static final AtomicLong furnaceOutputUnlocks = new AtomicLong();

    private JourneyRuntimeCounters() {}

    public static void panelIncrementalUpdate() {
        panelIncrementalUpdates.incrementAndGet();
    }

    public static void fullNeiReloadRequest() {
        fullNeiReloadRequests.incrementAndGet();
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

    public static Snapshot snapshot() {
        return new Snapshot(
            panelIncrementalUpdates.get(),
            fullNeiReloadRequests.get(),
            unlockNotifications.get(),
            furnaceOutputObservations.get(),
            furnaceOutputUnlocks.get());
    }

    public static void reset() {
        panelIncrementalUpdates.set(0L);
        fullNeiReloadRequests.set(0L);
        unlockNotifications.set(0L);
        furnaceOutputObservations.set(0L);
        furnaceOutputUnlocks.set(0L);
    }

    public static final class Snapshot {

        private final long panelIncrementalUpdates;
        private final long fullNeiReloadRequests;
        private final long unlockNotifications;
        private final long furnaceOutputObservations;
        private final long furnaceOutputUnlocks;

        private Snapshot(
            long panelIncrementalUpdates,
            long fullNeiReloadRequests,
            long unlockNotifications,
            long furnaceOutputObservations,
            long furnaceOutputUnlocks) {
            this.panelIncrementalUpdates = panelIncrementalUpdates;
            this.fullNeiReloadRequests = fullNeiReloadRequests;
            this.unlockNotifications = unlockNotifications;
            this.furnaceOutputObservations = furnaceOutputObservations;
            this.furnaceOutputUnlocks = furnaceOutputUnlocks;
        }

        public long getPanelIncrementalUpdates() {
            return panelIncrementalUpdates;
        }

        public long getFullNeiReloadRequests() {
            return fullNeiReloadRequests;
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
    }
}
