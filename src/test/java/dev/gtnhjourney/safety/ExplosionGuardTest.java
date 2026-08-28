package dev.gtnhjourney.safety;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.event.world.ExplosionEvent;

import org.junit.jupiter.api.Test;

class ExplosionGuardTest {

    @Test
    void enabledExplosionsRemainUncancelled() {
        ExplosionEvent.Start event = new ExplosionEvent.Start(null, null);

        ExplosionGuard.cancelBeforeDiagnostics(event, true, new Runnable() {

            @Override
            public void run() {
                throw new AssertionError("diagnostics must not run when explosions are enabled");
            }
        });

        assertFalse(event.isCanceled());
    }

    @Test
    void disabledExplosionsAreCancelledBeforeDiagnostics() {
        final ExplosionEvent.Start event = new ExplosionEvent.Start(null, null);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ExplosionGuard.cancelBeforeDiagnostics(
            event,
            false,
            new Runnable() {

                @Override
                public void run() {
                    assertTrue(event.isCanceled());
                    throw new RuntimeException("resolver failed");
                }
            }));

        assertTrue(event.isCanceled());
        assertTrue(thrown.getMessage().contains("resolver failed"));
    }
}
