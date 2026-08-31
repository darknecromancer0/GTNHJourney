package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DebugResearchCooldownTest {

    @Test
    public void rejectsRepeatScansForHalfASecondAndAcceptsAtTheBoundary() {
        DebugResearchCooldown cooldown = new DebugResearchCooldown();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        long start = 10_000_000_000L;

        assertTrue(cooldown.tryAcquire(player, start));
        assertFalse(cooldown.tryAcquire(player, start + 1L));
        assertFalse(cooldown.tryAcquire(player, start + 499_999_999L));
        assertTrue(cooldown.tryAcquire(player, start + 500_000_000L));
    }

    @Test
    public void playersHaveIndependentCooldowns() {
        DebugResearchCooldown cooldown = new DebugResearchCooldown();
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertTrue(cooldown.tryAcquire(first, 1_000L));
        assertTrue(cooldown.tryAcquire(second, 1_001L));
        assertFalse(cooldown.tryAcquire(first, 1_002L));
        assertFalse(cooldown.tryAcquire(second, 1_002L));
    }

    @Test
    public void aBackwardsMonotonicClockResetDoesNotLockTheToolForever() {
        DebugResearchCooldown cooldown = new DebugResearchCooldown();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertTrue(cooldown.tryAcquire(player, Long.MAX_VALUE - 10L));
        assertTrue(cooldown.tryAcquire(player, 20L));
    }
}
