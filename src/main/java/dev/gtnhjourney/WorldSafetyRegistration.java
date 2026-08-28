package dev.gtnhjourney;

/** Keeps world-safety handlers on the event bus that owns their event type. */
final class WorldSafetyRegistration {

    interface Registrar {

        void register(Object listener);
    }

    private WorldSafetyRegistration() {}

    static void register(Registrar fmlBus, Registrar forgeBus, Object backupTicker, Object explosionGuard) {
        fmlBus.register(backupTicker);
        forgeBus.register(explosionGuard);
    }
}
