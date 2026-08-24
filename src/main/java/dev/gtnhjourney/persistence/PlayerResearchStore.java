package dev.gtnhjourney.persistence;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import dev.gtnhjourney.research.ResearchKey;
import dev.gtnhjourney.research.ResearchRegistry;

/** Pure-Java per-player research container. Minecraft persistence wraps this class. */
public final class PlayerResearchStore {

    private final Map<UUID, ResearchRegistry> players = new LinkedHashMap<UUID, ResearchRegistry>();

    public ResearchRegistry forPlayer(UUID playerId) {
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        ResearchRegistry registry = players.get(playerId);
        if (registry == null) {
            registry = new ResearchRegistry();
            players.put(playerId, registry);
        }
        return registry;
    }

    public void restore(UUID playerId, Collection<ResearchKey> keys) {
        forPlayer(playerId).replaceAll(keys);
    }

    public int playerCount() {
        return players.size();
    }

    public void clearPlayer(UUID playerId) {
        if (playerId != null) players.remove(playerId);
    }

    public void clearAll() {
        players.clear();
    }
}
