package dev.gtnhjourney.backup;

import java.io.File;
import java.util.Date;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.config.JourneyConfig;

/** Owns backup cadence/session state and performs one fully synchronous saved-world archive operation. */
public final class WorldBackupCoordinator {

    private final Clock clock;
    private final Settings settings;
    private final BackupOperation operation;
    private volatile boolean running;
    private volatile boolean stopping;
    private volatile long lastSuccessfulBackupMillis;
    private volatile WorldBackupResult lastResult = WorldBackupResult.failure("No world backup completed this session.");

    public WorldBackupCoordinator() {
        this(new SystemClock(), new JourneySettings(), new MinecraftBackupOperation(new WorldArchiveWriter()));
    }

    WorldBackupCoordinator(Clock clock, Settings settings, BackupOperation operation) {
        if (clock == null || settings == null || operation == null) throw new IllegalArgumentException("backup dependencies");
        this.clock = clock;
        this.settings = settings;
        this.operation = operation;
        this.lastSuccessfulBackupMillis = clock.nowMillis();
    }

    public boolean isAutomaticDue() {
        return !running && !stopping && WorldBackupPolicy.isDue(
            clock.nowMillis(),
            lastSuccessfulBackupMillis,
            settings.intervalSeconds(),
            settings.enabled());
    }

    public synchronized WorldBackupResult tryBackup(MinecraftServer server, boolean manual) {
        if (stopping) return WorldBackupResult.failure("Backup skipped: server is stopping.");
        if (running) return WorldBackupResult.failure("Backup skipped: another backup is already running.");
        if (!manual && !settings.enabled()) return WorldBackupResult.failure("Backup skipped: automatic backups are disabled.");
        if (!manual && !WorldBackupPolicy.isDue(
            clock.nowMillis(),
            lastSuccessfulBackupMillis,
            settings.intervalSeconds(),
            true)) {
            return WorldBackupResult.failure("Backup skipped: automatic backup is not due yet.");
        }

        running = true;
        try {
            WorldBackupResult result;
            try {
                result = operation.run(server);
            } catch (RuntimeException failure) {
                result = WorldBackupResult.failure("Backup failed safely: " + safeMessage(failure));
            } catch (LinkageError failure) {
                result = WorldBackupResult.failure("Backup failed safely: " + safeMessage(failure));
            }
            if (result == null) result = WorldBackupResult.failure("Backup failed safely: no result.");
            lastResult = result;
            if (result.isSuccess()) lastSuccessfulBackupMillis = clock.nowMillis();
            return result;
        } finally {
            running = false;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public long lastSuccessfulBackupMillis() {
        return lastSuccessfulBackupMillis;
    }

    public WorldBackupResult lastResult() {
        return lastResult;
    }

    public void beginShutdown() {
        stopping = true;
    }

    public synchronized void resetSession() {
        running = false;
        stopping = false;
        lastSuccessfulBackupMillis = clock.nowMillis();
        lastResult = WorldBackupResult.failure("No world backup completed this session.");
    }

    static File instanceRootFor(File worldDir) {
        if (worldDir == null) return new File(".").getAbsoluteFile();
        File parent = worldDir.getAbsoluteFile().getParentFile();
        if (parent != null && "saves".equalsIgnoreCase(parent.getName()) && parent.getParentFile() != null) {
            return parent.getParentFile();
        }
        return parent == null ? new File(".").getAbsoluteFile() : parent;
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.length() == 0 ? failure.getClass().getSimpleName() : message;
    }

    interface Clock {

        long nowMillis();
    }

    interface Settings {

        boolean enabled();

        int intervalSeconds();

        int retention();
    }

    interface BackupOperation {

        WorldBackupResult run(MinecraftServer server);
    }

    private static final class SystemClock implements Clock {

        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }
    }

    private static final class JourneySettings implements Settings {

        @Override
        public boolean enabled() {
            return JourneyConfig.worldBackupsEnabled();
        }

        @Override
        public int intervalSeconds() {
            return JourneyConfig.worldBackupIntervalSeconds();
        }

        @Override
        public int retention() {
            return JourneyConfig.worldBackupRetention();
        }
    }

    private static final class MinecraftBackupOperation implements BackupOperation {

        private final WorldArchiveWriter writer;

        private MinecraftBackupOperation(WorldArchiveWriter writer) {
            this.writer = writer;
        }

        @Override
        public WorldBackupResult run(MinecraftServer server) {
            if (server == null || server.getConfigurationManager() == null) {
                return WorldBackupResult.failure("Backup failed safely: server is unavailable.");
            }

            try {
                server.getConfigurationManager().saveAllPlayerData();
                WorldServer[] worlds = server.worldServers;
                if (worlds == null || worlds.length == 0) {
                    return WorldBackupResult.failure("Backup failed safely: no loaded worlds.");
                }
                for (WorldServer world : worlds) {
                    if (world != null) world.saveAllChunks(true, null);
                }

                ThreadedFileIOBase.threadedIOInstance.waitForFinish();

                File worldDir = DimensionManager.getCurrentSaveRootDirectory();
                if (worldDir == null || !worldDir.isDirectory()) {
                    return WorldBackupResult.failure("Backup failed safely: save root is unavailable.");
                }
                File backupDir = WorldBackupPaths.backupRoot(instanceRootFor(worldDir), worldDir.getName());
                return writer.write(worldDir, backupDir, worldDir.getName(), JourneyConfig.worldBackupRetention(), new Date());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return WorldBackupResult.failure("Backup failed safely: interrupted while flushing world data.");
            } catch (Exception failure) {
                return WorldBackupResult.failure("Backup failed safely: " + safeMessage(failure));
            } catch (LinkageError failure) {
                return WorldBackupResult.failure("Backup failed safely: " + safeMessage(failure));
            }
        }
    }
}
