package dev.gtnhjourney.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExplosionNotificationThrottleTest {

    @Test
    void aggregatesSuppressedExplosionsForFiveSeconds() {
        ExplosionNotificationThrottle throttle = new ExplosionNotificationThrottle(5000L);

        assertTrue(throttle.record(0L).shouldNotify());
        assertFalse(throttle.record(1000L).shouldNotify());
        assertFalse(throttle.record(4000L).shouldNotify());

        ExplosionNotificationThrottle.Decision decision = throttle.record(5000L);
        assertTrue(decision.shouldNotify());
        assertEquals(2, decision.suppressedBeforeThis());
    }

    @Test
    void resetStartsANewNotificationWindow() {
        ExplosionNotificationThrottle throttle = new ExplosionNotificationThrottle(5000L);
        throttle.record(100L);
        throttle.record(1000L);

        throttle.reset();

        ExplosionNotificationThrottle.Decision decision = throttle.record(1001L);
        assertTrue(decision.shouldNotify());
        assertEquals(0, decision.suppressedBeforeThis());
    }
}
