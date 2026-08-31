package dev.gtnhjourney.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player real-time debounce for the migration researcher tool. */
final class DebugResearchCooldown {

    static final long COOLDOWN_NANOS = 500_000_000L;
    private final Map<UUID, Long> lastAcceptedNanos = new HashMap<UUID, Long>();

    boolean tryAcquire(UUID playerId, long nowNanos) {
        if (playerId == null) return false;
        Long previous = lastAcceptedNanos.get(playerId);
        if (previous != null) {
            long elapsed = nowNanos - previous.longValue();
            if (elapsed >= 0L && elapsed < COOLDOWN_NANOS) return false;
        }
        lastAcceptedNanos.put(playerId, Long.valueOf(nowNanos));
        return true;
    }
}
