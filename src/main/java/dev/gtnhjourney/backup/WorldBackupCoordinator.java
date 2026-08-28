package dev.gtnhjourney.backup;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.config.JourneyConfig;

/** Owns backup cadence/session state and splits save preparation from background archive work. */
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
                prepared.restore();
            } catch (RuntimeException failure) {
                result = WorldBackupResult.failure("Backup finished, but save-state restore failed: " + safeMessage(failure));
            } catch (LinkageError failure) {
                result = WorldBackupResult.failure("Backup finished, but save-state restore failed: " + safeMessage(failure));
            }
        }

        long now = clock.nowMillis();
        lastDurationMillis = Math.max(0L, now - backupStartedMillis);
        lastResult = result;
        if (result.isSuccess()) lastSuccessfulBackupMillis = now;
        clearActiveBackup();
        return result;
    }

    /** Stops accepting new backups and safely drains an active worker before the server saves during shutdown. */
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
                prepared.restore();
            } catch (RuntimeException restoreFailure) {
                message = message + " Save-state restore also failed: " + safeMessage(restoreFailure);
            } catch (LinkageError restoreFailure) {
                message = message + " Save-state restore also failed: " + safeMessage(restoreFailure);
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

        void restore();
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

            List<WorldSaveState> saveStates = new ArrayList<WorldSaveState>();
            try {
                server.getConfigurationManager().saveAllPlayerData();
                WorldServer[] worlds = server.worldServers;
                if (worlds == null || worlds.length == 0) throw new IllegalStateException("no loaded worlds");

                for (WorldServer world : worlds) {
                    if (world == null) continue;
                    saveStates.add(new WorldSaveState(world, world.levelSaving));
                    world.saveAllChunks(true, null);
                    world.levelSaving = true;
                }

                ThreadedFileIOBase.threadedIOInstance.waitForFinish();

                File worldDir = DimensionManager.getCurrentSaveRootDirectory();
                if (worldDir == null || !worldDir.isDirectory()) throw new IllegalStateException("save root is unavailable");
                File backupDir = WorldBackupPaths.backupRoot(instanceRootFor(worldDir), worldDir.getName());
                return new MinecraftPreparedBackup(
                    writer,
                    worldDir,
                    backupDir,
                    JourneyConfig.worldBackupRetention(),
                    new Date(),
                    saveStates);
            } catch (InterruptedException interrupted) {
                restoreSaveStates(saveStates);
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (Exception failure) {
                restoreSaveStates(saveStates);
                throw failure;
            } catch (LinkageError failure) {
                restoreSaveStates(saveStates);
                throw failure;
            }
        }
    }

    private static final class MinecraftPreparedBackup implements PreparedBackup {

        private final WorldArchiveWriter writer;
        private final File worldDir;
        private final File backupDir;
        private final int retention;
        private final Date timestamp;
        private final List<WorldSaveState> saveStates;

        private MinecraftPreparedBackup(
            WorldArchiveWriter writer,
            File worldDir,
            File backupDir,
            int retention,
            Date timestamp,
            List<WorldSaveState> saveStates) {
            this.writer = writer;
            this.worldDir = worldDir;
            this.backupDir = backupDir;
            this.retention = retention;
            this.timestamp = timestamp;
            this.saveStates = new ArrayList<WorldSaveState>(saveStates);
        }

        @Override
        public WorldBackupResult archive() {
            return writer.write(worldDir, backupDir, worldDir.getName(), retention, timestamp);
        }

        @Override
        public void restore() {
            restoreSaveStates(saveStates);
        }
    }

    private static final class WorldSaveState {

        private final WorldServer world;
        private final boolean levelSaving;

        private WorldSaveState(WorldServer world, boolean levelSaving) {
            this.world = world;
            this.levelSaving = levelSaving;
        }
    }

    private static void restoreSaveStates(List<WorldSaveState> saveStates) {
        for (WorldSaveState state : saveStates) {
            if (state != null && state.world != null) state.world.levelSaving = state.levelSaving;
        }
    }
}
