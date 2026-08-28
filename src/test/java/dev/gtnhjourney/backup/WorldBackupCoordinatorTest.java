package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.server.MinecraftServer;

import org.junit.jupiter.api.Test;

class WorldBackupCoordinatorTest {

    @Test
    void automaticBackupUsesWallClockAndManualBypassesDisabledSetting() {
        MutableClock clock = new MutableClock();
        MutableSettings settings = new MutableSettings(true, 300);
        CountingOperation operation = new CountingOperation();
        WorldBackupCoordinator coordinator = new WorldBackupCoordinator(clock, settings, operation);

        clock.now = 299_999L;
        assertFalse(coordinator.isAutomaticDue());
        clock.now = 300_000L;
        assertTrue(coordinator.isAutomaticDue());
        assertTrue(coordinator.tryBackup(null, false).isSuccess());
        assertEquals(1, operation.calls);
        assertEquals(300_000L, coordinator.lastSuccessfulBackupMillis());

        settings.enabled = false;
        clock.now = 600_000L;
        assertFalse(coordinator.isAutomaticDue());
        assertFalse(coordinator.tryBackup(null, false).isSuccess());
        assertEquals(1, operation.calls);
        assertTrue(coordinator.tryBackup(null, true).isSuccess());
        assertEquals(2, operation.calls);
    }

    @Test
    void failedBackupDoesNotAdvanceSuccessfulCadence() {
        MutableClock clock = new MutableClock();
        WorldBackupCoordinator.BackupOperation failure = new WorldBackupCoordinator.BackupOperation() {

            @Override
            public WorldBackupResult run(MinecraftServer server) {
                return WorldBackupResult.failure("synthetic failure");
            }
        };
        WorldBackupCoordinator coordinator = new WorldBackupCoordinator(clock, new MutableSettings(true, 300), failure);
        clock.now = 300_000L;

        assertFalse(coordinator.tryBackup(null, false).isSuccess());
        assertEquals(0L, coordinator.lastSuccessfulBackupMillis());
        assertTrue(coordinator.isAutomaticDue());
    }

    @Test
    void runningCoordinatorRejectsOverlappingRequest() {
        MutableClock clock = new MutableClock();
        MutableSettings settings = new MutableSettings(true, 300);
        final WorldBackupCoordinator[] holder = new WorldBackupCoordinator[1];
        WorldBackupCoordinator.BackupOperation nested = new WorldBackupCoordinator.BackupOperation() {

            @Override
            public WorldBackupResult run(MinecraftServer server) {
                assertTrue(holder[0].isRunning());
                assertFalse(holder[0].tryBackup(server, true).isSuccess());
                return WorldBackupResult.success(null);
            }
        };
        holder[0] = new WorldBackupCoordinator(clock, settings, nested);
        clock.now = 300_000L;

        assertTrue(holder[0].tryBackup(null, false).isSuccess());
        assertFalse(holder[0].isRunning());
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

    private static final class CountingOperation implements WorldBackupCoordinator.BackupOperation {

        private int calls;

        @Override
        public WorldBackupResult run(MinecraftServer server) {
            calls++;
            return WorldBackupResult.success(null);
        }
    }
}
