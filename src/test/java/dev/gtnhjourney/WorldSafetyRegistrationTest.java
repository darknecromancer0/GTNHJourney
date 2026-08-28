package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class WorldSafetyRegistrationTest {

    @Test
    void routesBackupTickerToFmlBusAndExplosionGuardToForgeBus() {
        final List<Object> fmlListeners = new ArrayList<Object>();
        final List<Object> forgeListeners = new ArrayList<Object>();
        Object backupTicker = new Object();
        Object explosionGuard = new Object();

        WorldSafetyRegistration.register(
            new WorldSafetyRegistration.Registrar() {

                @Override
                public void register(Object listener) {
                    fmlListeners.add(listener);
                }
            },
            new WorldSafetyRegistration.Registrar() {

                @Override
                public void register(Object listener) {
                    forgeListeners.add(listener);
                }
            },
            backupTicker,
            explosionGuard);

        assertEquals(Collections.singletonList(backupTicker), fmlListeners);
        assertEquals(Collections.singletonList(explosionGuard), forgeListeners);
    }
}
