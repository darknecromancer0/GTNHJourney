package dev.gtnhjourney.acquisition;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Coalesces event hints into one forced authoritative inventory reconciliation per player. */
public final class ReconcileRequestSet {

    private final Set<UUID> pending = new HashSet<UUID>();

    public synchronized void request(UUID playerId) {
        if (playerId != null) pending.add(playerId);
    }

    public synchronized boolean consume(UUID playerId) {
        return playerId != null && pending.remove(playerId);
    }

    public synchronized void discard(UUID playerId) {
        if (playerId != null) pending.remove(playerId);
    }

    public synchronized void clear() {
        pending.clear();
    }
}
