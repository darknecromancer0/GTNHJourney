package dev.gtnhjourney.backup;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.FMLLog;

/**
 * Compatibility guard for ServerUtilities versions that suppress normal world saving while their asynchronous backup
 * worker is copying the save directory, but do not reliably restore that state when the integrated server stops.
 *
 * <p>This deliberately uses reflection so GTNH Journey does not gain a hard ServerUtilities dependency. ServerUtilities
 * 2.4.1 stores the previous {@code WorldServer.levelSaving} values by index in {@code BackupTask.dimSaveStates}, then
 * sets each world to {@code levelSaving = true}. If Save & Quit happens before {@code postBackup()}, Minecraft's final
 * save pass is skipped even though it logs "Saving chunks...".</p>
 */
public final class ServerUtilitiesBackupSaveSafety {

    private static final String BACKUP_TASK_CLASS = "serverutils.task.backup.BackupTask";
    private static final String THREAD_FIELD = "thread";
    private static final String SAVE_STATES_FIELD = "dimSaveStates";
    private static final long SHUTDOWN_WAIT_MILLIS = 60_000L;

    private ServerUtilitiesBackupSaveSafety() {}

    public static void prepareForServerStopping() {
        try {
            Class<?> backupTask = Class.forName(
                BACKUP_TASK_CLASS,
                false,
                ServerUtilitiesBackupSaveSafety.class.getClassLoader());
            Field threadField = accessibleField(backupTask, THREAD_FIELD);
            Field saveStatesField = accessibleField(backupTask, SAVE_STATES_FIELD);

            Thread nativeWorker = asThread(threadField.get(null));
            Object capturedSaveStates = saveStatesField.get(null);
            boolean suppressedWorlds = hasSuppressedWorlds(capturedSaveStates);

            if (nativeWorker == null && !suppressedWorlds) return;

            boolean waitedForWorker = false;
            if (nativeWorker != null && nativeWorker.isAlive()) {
                waitedForWorker = true;
                FMLLog.info(
                    "[GTNH Journey] Save & Quit detected an active ServerUtilities backup; waiting up to %d ms before final world save.",
                    SHUTDOWN_WAIT_MILLIS);
                nativeWorker.interrupt();
                join(nativeWorker, SHUTDOWN_WAIT_MILLIS);
            }

            boolean workerStillAlive = nativeWorker != null && nativeWorker.isAlive();
            int restoredWorlds = restoreCapturedSaveStates(capturedSaveStates);

            // A dead worker reference blocks later 2.4.1 backups. If the legacy worker ignored interruption and exceeded
            // the shutdown bound, live-world persistence takes priority over the now potentially inconsistent archive.
            Object currentWorker = threadField.get(null);
            if (currentWorker == nativeWorker && (nativeWorker == null || !nativeWorker.isAlive() || restoredWorlds > 0)) {
                threadField.set(null, null);
            }

            if (restoredWorlds > 0) {
                FMLLog.info(
                    "[GTNH Journey] Restored ServerUtilities save state for %d world(s) before shutdown save.",
                    restoredWorlds);
            }
            if (workerStillAlive) {
                FMLLog.warning(
                    "[GTNH Journey] ServerUtilities backup did not stop within %d ms; final world saving was re-enabled to preserve live progress. The in-flight backup archive may be incomplete.",
                    SHUTDOWN_WAIT_MILLIS);
            } else if (waitedForWorker) {
                FMLLog.info("[GTNH Journey] ServerUtilities backup worker finished/stopped before final world save.");
            }
        } catch (ClassNotFoundException ignored) {
            // ServerUtilities is optional.
        } catch (Throwable error) {
            FMLLog.warning(
                "[GTNH Journey] Could not repair ServerUtilities backup save state before shutdown: %s",
                error.toString());
        }
    }

    static int restoreCapturedSaveStates(Object capturedSaveStates) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null) return 0;

        int restored = 0;
        for (int i = 0; i < server.worldServers.length; i++) {
            WorldServer world = server.worldServers[i];
            if (world == null) continue;

            Boolean previous = capturedState(capturedSaveStates, i);
            if (previous == null || world.levelSaving == previous.booleanValue()) continue;

            world.levelSaving = previous.booleanValue();
            restored++;
        }
        return restored;
    }

    private static boolean hasSuppressedWorlds(Object capturedSaveStates) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null) return false;

        for (int i = 0; i < server.worldServers.length; i++) {
            WorldServer world = server.worldServers[i];
            if (world == null) continue;
            Boolean previous = capturedState(capturedSaveStates, i);
            if (previous != null && world.levelSaving != previous.booleanValue()) return true;
        }
        return false;
    }

    private static Boolean capturedState(Object capturedSaveStates, int index) {
        if (!(capturedSaveStates instanceof Map<?, ?>)) return null;
        Map<?, ?> states = (Map<?, ?>) capturedSaveStates;
        Object key = Integer.valueOf(index);
        if (!states.containsKey(key)) return null;
        Object value = states.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private static Field accessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Thread asThread(Object value) {
        return value instanceof Thread ? (Thread) value : null;
    }

    private static void join(Thread thread, long timeoutMillis) {
        try {
            thread.join(timeoutMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
