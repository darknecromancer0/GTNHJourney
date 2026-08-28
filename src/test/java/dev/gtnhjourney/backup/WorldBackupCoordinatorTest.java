package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.server.MinecraftServer;

import org.junit.jupiter.api.Test;

class WorldBackupCoordinatorTest {

    @Test
    void archiveRunsOnWorkerAndRestoresWorldStateOnlyWhenServerPollsCompletion() {
        MutableClock clock = new MutableClock();
        MutableSettings settings = new MutableSettings(true, 300);
        RecordingPreparedBackup prepared = new RecordingPreparedBackup(WorldBackupResult.success(null));
        RecordingOperation operation = new RecordingOperation(prepared);
        ManualWorkerLauncher launcher = new ManualWorkerLauncher();
        WorldBackupCoordinator coordinator = new WorldBackupCoordinator(clock, settings, operation, launcher);

        clock.now = 100_000L;
        coordinator.markWorldLoaded();
        clock.now = 400_000L;
        WorldBackupResult started = coordinator.tryBackup(null, false);

        assertTrue(started.isSuccess());
        assertTrue(started.getMessage().toLowerCase(java.util.Locale.ROOT).contains("started"));
        assertTrue(coordinator.isRunning());
        assertEquals(1, operation.prepareCalls);
        assertEquals(0, prepared.archiveCalls);
        assertEquals(0, prepared.restoreCalls);
        assertNull(coordinator.pollCompletion());

        launcher.runPending();
        assertEquals(1, prepared.archiveCalls);
        assertTrue(coordinator.isRunning());
        assertEquals(0, prepared.restoreCalls);

        clock.now = 405_000L;
        WorldBackupResult completed = coordinator.pollCompletion();
        assertNotNull(completed);
        assertTrue(completed.isSuccess());
        assertEquals(1, prepared.restoreCalls);
        assertFalse(coordinator.isRunning());
        assertEquals(5_000L, coordinator.lastDurationMillis());
        assertEquals(405_000L, coordinator.lastSuccessfulBackupMillis());
    }

    @Test
    void worldLoadRestartsAutomaticCadenceAndManualStillBypassesDisabledSetting() {
        MutableClock clock = new MutableClock();
        MutableSettings settings = new MutableSettings(true, 300);
        RecordingPreparedBackup first = new RecordingPreparedBackup(WorldBackupResult.success(null));
        RecordingOperation operation = new RecordingOperation(first);
        ManualWorkerLauncher launcher = new ManualWorkerLauncher();
        WorldBackupCoordinator coordinator = new WorldBackupCoordinator(clock, settings, operation, launcher);

        clock.now = 100_000L;
        coordinator.markWorldLoaded();
        clock.now = 399_999L;
        assertFalse(coordinator.isAutomaticDue());
        clock.now = 400_000L;
        assertTrue(coordinator.isAutomaticDue());

        settings.enabled = false;
        assertFalse(coordinator.isAutomaticDue());
        assertFalse(coordinator.tryBackup(null, false).isSuccess());

        assertTrue(coordinator.tryBackup(null, true).isSuccess());
        assertTrue(coordinator.isRunning());
        launcher.runPending();
        assertNotNull(coordinator.pollCompletion());
        assertFalse(coordinator.isRunning());
    }

    @Test
    void failedWorkerBackupDoesNotAdvanceSuccessfulCadence() {
        MutableClock clock = new MutableClock();
        RecordingPreparedBackup prepared = new RecordingPreparedBackup(WorldBackupResult.failure("synthetic failure"));
        ManualWorkerLauncher launcher = new ManualWorkerLauncher();
        WorldBackupCoordinator coordinator = new WorldBackupCoordinator(
            clock,
            new MutableSettings(true, 300),
            new RecordingOperation(prepared),
            launcher);
        coordinator.markWorldLoaded();
        clock.now = 300_000L;

        assertTrue(coordinator.tryBackup(null, false).isSuccess());
        launcher.runPending();
        WorldBackupResult completed = coordinator.pollCompletion();

        assertNotNull(completed);
        assertFalse(completed.isSuccess());
        assertEquals(0L, coordinator.lastSuccessfulBackupMillis());
        assertEquals(1, prepared.restoreCalls);
        assertTrue(coordinator.isAutomaticDue());
    }

    @Test
    void runningCoordinatorRejectsOverlappingRequestUntilCompletionIsPolled() {
        MutableClock clock = new MutableClock();
        RecordingPreparedBackup prepared = new RecordingPreparedBackup(WorldBackupResult.success(null));
        ManualWorkerLauncher launcher = new ManualWorkerLauncher();
        WorldBackupCoordinator coordinator = new WorldBackupCoordinator(
            clock,
            new MutableSettings(true, 300),
            new RecordingOperation(prepared),
            launcher);
        clock.now = 300_000L;

        assertTrue(coordinator.tryBackup(null, false).isSuccess());
        assertTrue(coordinator.isRunning());
        assertFalse(coordinator.tryBackup(null, true).isSuccess());
        launcher.runPending();
        assertTrue(coordinator.isRunning());
        assertNotNull(coordinator.pollCompletion());
        assertFalse(coordinator.isRunning());
    }

    private static final class MutableClock implements WorldBackupCoordinator.Clock {

        private long now;

        @Override
        public long nowMillis() {
            return now;
        }
    }

    private static final class MutableSettings implements WorldBackupCoordinator.Settings {

        private boolean enabled;
        private final int intervalSeconds;

        private MutableSettings(boolean enabled, int intervalSeconds) {
            this.enabled = enabled;
            this.intervalSeconds = intervalSeconds;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public int intervalSeconds() {
            return intervalSeconds;
        }

        @Override
        public int retention() {
            return 3;
        }
    }

    private static final class RecordingOperation implements WorldBackupCoordinator.BackupOperation {

        private final WorldBackupCoordinator.PreparedBackup prepared;
        private int prepareCalls;

        private RecordingOperation(WorldBackupCoordinator.PreparedBackup prepared) {
            this.prepared = prepared;
        }

        @Override
        public WorldBackupCoordinator.PreparedBackup prepare(MinecraftServer server) {
            prepareCalls++;
            return prepared;
        }
    }

    private static final class RecordingPreparedBackup implements WorldBackupCoordinator.PreparedBackup {

        private final WorldBackupResult result;
        private int archiveCalls;
        private int restoreCalls;

        private RecordingPreparedBackup(WorldBackupResult result) {
            this.result = result;
        }

        @Override
        public WorldBackupResult archive() {
            archiveCalls++;
            return result;
        }

        @Override
        public void restore() {
            restoreCalls++;
        }
    }

    private static final class ManualWorkerLauncher implements WorldBackupCoordinator.WorkerLauncher {

        private Runnable pending;

        @Override
        public void launch(Runnable task) {
            if (pending != null) throw new IllegalStateException("worker already queued");
            pending = task;
        }

        private void runPending() {
            Runnable task = pending;
            pending = null;
            assertNotNull(task);
            task.run();
        }
    }
}
