package dev.gtnhjourney.backup;

import java.io.File;
import java.util.Date;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.config.JourneyConfig;

/** Owns backup cadence/session state and splits stable snapshot staging from background archive work. */
public final class WorldBackupCoordinator {

    private final Clock clock;
    private final Settings settings;
    private final BackupOperation operation;
    private final WorkerLauncher workerLauncher;
    private volatile boolean running;
    private volatile boolean stopping;
    private volatile boolean workerFinished;
    private volatile long lastSuccessfulBackupMillis;
    private volatile long backupStartedMillis;
    private volatile long lastDurationMillis = -1L;
    private volatile WorldBackupResult lastResult = WorldBackupResult.failure("No world backup completed this session.");
    private volatile WorldBackupResult workerResult;
    private PreparedBackup activeBackup;

    public WorldBackupCoordinator() {
        this(new SystemClock(), new JourneySettings(), new MinecraftBackupOperation(new WorldArchiveWriter()), new ThreadWorkerLauncher());
    }

    WorldBackupCoordinator(Clock clock, Settings settings, BackupOperation operation, WorkerLauncher workerLauncher) {
        if (clock == null || settings == null || operation == null || workerLauncher == null) {
            throw new IllegalArgumentException("backup dependencies");
        }
        this.clock = clock;
        this.settings = settings;
        this.operation = operation;
        this.workerLauncher = workerLauncher;
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
        workerFinished = false;
        workerResult = null;
        backupStartedMillis = clock.nowMillis();
        PreparedBackup prepared = null;
        try {
            prepared = operation.prepare(server);
            if (prepared == null) throw new IllegalStateException("backup preparation returned no work");
            activeBackup = prepared;
            final PreparedBackup workerBackup = prepared;
            workerLauncher.launch(new Runnable() {

                @Override
                public void run() {
                    WorldBackupResult result;
                    try {
                        result = workerBackup.archive();
                    } catch (RuntimeException failure) {
                        result = WorldBackupResult.failure("Backup failed safely: " + safeMessage(failure));
                    } catch (LinkageError failure) {
                        result = WorldBackupResult.failure("Backup failed safely: " + safeMessage(failure));
                    }
                    if (result == null) result = WorldBackupResult.failure("Backup failed safely: no result.");
                    synchronized (WorldBackupCoordinator.this) {
                        workerResult = result;
                        workerFinished = true;
                        WorldBackupCoordinator.this.notifyAll();
                    }
                }
            });
            return WorldBackupResult.started();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return failStart(prepared, "Backup failed safely: interrupted while flushing world data.");
        } catch (Exception failure) {
            return failStart(prepared, "Backup failed safely: " + safeMessage(failure));
        } catch (LinkageError failure) {
            return failStart(prepared, "Backup failed safely: " + safeMessage(failure));
        }
    }

    public synchronized WorldBackupResult pollCompletion() {
        if (!running || !workerFinished) return null;

        WorldBackupResult result = workerResult;
        if (result == null) result = WorldBackupResult.failure("Backup failed safely: no worker result.");
        PreparedBackup prepared = activeBackup;
        if (prepared != null) {
            try {
                prepared.cleanup();
            } catch (Exception failure) {
                if (result.isSuccess()) {
                    result = WorldBackupResult.failure("Backup archive completed, but staging cleanup failed: " + safeMessage(failure));
                }
            } catch (LinkageError failure) {
                if (result.isSuccess()) {
                    result = WorldBackupResult.failure("Backup archive completed, but staging cleanup failed: " + safeMessage(failure));
                }
            }
        }

        long now = clock.nowMillis();
        lastDurationMillis = Math.max(0L, now - backupStartedMillis);
        lastResult = result;
        if (result.isSuccess()) lastSuccessfulBackupMillis = now;
        clearActiveBackup();
        return result;
    }

    /** Stops accepting new backups and safely drains an active archive worker before final shutdown. */
    public synchronized WorldBackupResult finishForShutdown() {
        stopping = true;
        boolean interrupted = false;
        while (running && !workerFinished) {
            try {
                wait();
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }

        WorldBackupResult result = running ? pollCompletion() : null;
        if (interrupted) Thread.currentThread().interrupt();
        return result;
    }

    private WorldBackupResult failStart(PreparedBackup prepared, String message) {
        if (prepared != null) {
            try {
                prepared.cleanup();
            } catch (Exception cleanupFailure) {
                message = message + " Staging cleanup also failed: " + safeMessage(cleanupFailure);
            } catch (LinkageError cleanupFailure) {
                message = message + " Staging cleanup also failed: " + safeMessage(cleanupFailure);
            }
        }
        WorldBackupResult result = WorldBackupResult.failure(message);
        lastDurationMillis = Math.max(0L, clock.nowMillis() - backupStartedMillis);
        lastResult = result;
        clearActiveBackup();
        return result;
    }

    private void clearActiveBackup() {
        running = false;
        workerFinished = false;
        workerResult = null;
        activeBackup = null;
    }

    public boolean isRunning() {
        return running;
    }

    public long lastSuccessfulBackupMillis() {
        return lastSuccessfulBackupMillis;
    }

    public long lastDurationMillis() {
        return lastDurationMillis;
    }

    public WorldBackupResult lastResult() {
        return lastResult;
    }

    public synchronized void markWorldLoaded() {
        if (running) return;
        stopping = false;
        lastSuccessfulBackupMillis = clock.nowMillis();
        lastDurationMillis = -1L;
        lastResult = WorldBackupResult.failure("No world backup completed this session.");
    }

    public void beginShutdown() {
        stopping = true;
    }

    public synchronized void resetSession() {
        running = false;
        stopping = false;
        workerFinished = false;
        workerResult = null;
        activeBackup = null;
        lastSuccessfulBackupMillis = clock.nowMillis();
        lastDurationMillis = -1L;
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

        PreparedBackup prepare(MinecraftServer server) throws Exception;
    }

    interface PreparedBackup {

        WorldBackupResult archive();

        void cleanup() throws Exception;
    }

    interface WorkerLauncher {

        void launch(Runnable task);
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

    private static final class ThreadWorkerLauncher implements WorkerLauncher {

        @Override
        public void launch(Runnable task) {
            Thread worker = new Thread(task, "GTNHJourney-WorldBackup");
            worker.setDaemon(true);
            worker.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            worker.start();
        }
    }

    private static final class MinecraftBackupOperation implements BackupOperation {

        private final WorldArchiveWriter writer;

        private MinecraftBackupOperation(WorldArchiveWriter writer) {
            this.writer = writer;
        }

        @Override
        public PreparedBackup prepare(MinecraftServer server) throws Exception {
            if (server == null || server.getConfigurationManager() == null) {
                throw new IllegalStateException("server is unavailable");
            }

            server.getConfigurationManager().saveAllPlayerData();
            WorldServer[] worlds = server.worldServers;
            if (worlds == null || worlds.length == 0) throw new IllegalStateException("no loaded worlds");

            for (WorldServer world : worlds) {
                if (world != null) world.saveAllChunks(true, null);
            }
            ThreadedFileIOBase.threadedIOInstance.waitForFinish();

            File worldDir = DimensionManager.getCurrentSaveRootDirectory();
            if (worldDir == null || !worldDir.isDirectory()) throw new IllegalStateException("save root is unavailable");
            File backupDir = WorldBackupPaths.backupRoot(instanceRootFor(worldDir), worldDir.getName());
            WorldSnapshotStager.StagedSnapshot staged = WorldSnapshotStager.stage(worldDir, backupDir);
            return new MinecraftPreparedBackup(
                writer,
                staged,
                backupDir,
                JourneyConfig.worldBackupRetention(),
                new Date());
        }
    }

    private static final class MinecraftPreparedBackup implements PreparedBackup {

        private final WorldArchiveWriter writer;
        private final WorldSnapshotStager.StagedSnapshot staged;
        private final File backupDir;
        private final int retention;
        private final Date timestamp;

        private MinecraftPreparedBackup(
            WorldArchiveWriter writer,
            WorldSnapshotStager.StagedSnapshot staged,
            File backupDir,
            int retention,
            Date timestamp) {
            this.writer = writer;
            this.staged = staged;
            this.backupDir = backupDir;
            this.retention = retention;
            this.timestamp = timestamp;
        }

        @Override
        public WorldBackupResult archive() {
            File stagedWorld = staged.worldDirectory();
            return writer.write(stagedWorld, backupDir, stagedWorld.getName(), retention, timestamp);
        }

        @Override
        public void cleanup() throws Exception {
            staged.cleanup();
        }
    }
}
