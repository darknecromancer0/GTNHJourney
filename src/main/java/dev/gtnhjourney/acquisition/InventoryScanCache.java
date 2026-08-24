package dev.gtnhjourney.acquisition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player cheap slot-signature cache used to avoid rebuilding full Journey identity for stable inventory slots.
 * A periodic forced pass remains the safety net for hash collisions and exotic in-place mutations.
 */
public final class InventoryScanCache {
    private final Map<String, Integer> previous = new HashMap<String, Integer>();
    private final Set<String> seen = new HashSet<String>();
    private boolean force;
    private boolean passOpen;

    public void beginPass(boolean force) {
        this.force = force;
        seen.clear();
        passOpen = true;
    }

    public boolean shouldInspect(String slotId, int signature) {
        if (!passOpen) throw new IllegalStateException("beginPass must be called first");
        if (slotId == null) throw new IllegalArgumentException("slotId must not be null");
        seen.add(slotId);
        Integer old = previous.put(slotId, Integer.valueOf(signature));
        return force || old == null || old.intValue() != signature;
    }

    public void endPass() {
        if (!passOpen) return;
        previous.keySet().retainAll(seen);
        seen.clear();
        force = false;
        passOpen = false;
    }

    public void clear() {
        previous.clear();
        seen.clear();
        force = false;
        passOpen = false;
    }

    public int size() { return previous.size(); }
}
