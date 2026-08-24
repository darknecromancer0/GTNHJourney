package dev.gtnhjourney.client;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Executes client-bound Journey network mutations on the client tick thread instead of Netty's callback thread. */
public final class ClientNetworkQueue {
    private static final int MAX_TASKS_PER_TICK = 256;
    private static final ClientTaskQueue TASKS = new ClientTaskQueue();
    private static final ClientSessionGate SESSION = new ClientSessionGate();
    private static int loggedFailures;

    public static void enqueue(Runnable task) {
        if (!SESSION.isActive()) return;
        TASKS.enqueue(task);
    }

    /** Starts a fresh connection session and puts bootstrap cleanup ahead of every packet from that session. */
    public static void beginSession(Runnable bootstrap) {
        SESSION.end();
        TASKS.clear();
        loggedFailures = 0;
        if (bootstrap != null) TASKS.enqueue(bootstrap);
        SESSION.begin();
    }

    /** Rejects late packets immediately, then schedules client-only cleanup on the next client tick. */
    public static void endSession(Runnable cleanup) {
        SESSION.end();
        TASKS.clear();
        loggedFailures = 0;
        if (cleanup != null) TASKS.enqueue(cleanup);
    }

    public static void clear() {
        SESSION.end();
        TASKS.clear();
        loggedFailures = 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        TASKS.drainSafely(MAX_TASKS_PER_TICK, new ClientTaskQueue.FailureHandler() {
            @Override public void failed(Throwable failure) {
                if (loggedFailures++ >= 8) return;
                String message = failure == null ? "<unknown>" : failure.getClass().getName()
                    + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
                FMLLog.warning("[GTNH Journey] Skipping broken client sync task: %s", message);
            }
        });
    }
}
