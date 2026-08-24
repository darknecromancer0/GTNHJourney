package dev.gtnhjourney.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Small pure-Java guard preventing one client from filling the global retrieval queue. */
public final class PendingRequestLimiter {

    private final int maxPendingPerPlayer;
    private final Map<UUID, Integer> pending = new HashMap<UUID, Integer>();

    public PendingRequestLimiter(int maxPendingPerPlayer) {
        if (maxPendingPerPlayer < 1) throw new IllegalArgumentException("maxPendingPerPlayer must be positive");
        this.maxPendingPerPlayer = maxPendingPerPlayer;
    }

    public synchronized boolean tryAcquire(UUID playerId) {
        if (playerId == null) return false;
        Integer current = pending.get(playerId);
        int count = current == null ? 0 : current.intValue();
        if (count >= maxPendingPerPlayer) return false;
        pending.put(playerId, Integer.valueOf(count + 1));
        return true;
    }

    public synchronized void release(UUID playerId) {
        if (playerId == null) return;
        Integer current = pending.get(playerId);
        if (current == null || current.intValue() <= 1) pending.remove(playerId);
        else pending.put(playerId, Integer.valueOf(current.intValue() - 1));
    }

    public synchronized int pending(UUID playerId) {
        Integer current = pending.get(playerId);
        return current == null ? 0 : current.intValue();
    }

    public synchronized void clear() {
        pending.clear();
    }
}
