package dev.gtnhjourney.research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Orders researched states by meaningful Journey activity. First research appends once; successful Journey retrieval
 * moves an existing state to the newest position. Re-observing an already researched item does not change the order.
 */
public final class ResearchActivityTimeline {

    private final LinkedHashSet<ResearchKey> oldestFirst = new LinkedHashSet<ResearchKey>();

    public boolean recordUnlock(ResearchKey key) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        return oldestFirst.add(key);
    }

    public boolean recordRetrieval(ResearchKey key) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        boolean wasNewest = false;
        if (!oldestFirst.isEmpty()) {
            ResearchKey newest = null;
            for (ResearchKey candidate : oldestFirst) newest = candidate;
            wasNewest = key.equals(newest);
        }
        if (wasNewest) return false;
        oldestFirst.remove(key);
        oldestFirst.add(key);
        return true;
    }

    public boolean remove(ResearchKey key) {
        return key != null && oldestFirst.remove(key);
    }

    public void restore(Collection<ResearchKey> keys) {
        oldestFirst.clear();
        if (keys == null) return;
        for (ResearchKey key : keys) if (key != null) oldestFirst.add(key);
    }

    /** Ensures N can never lose a researched state even when loading legacy or partially written activity data. */
    public boolean appendMissing(Collection<ResearchKey> researchOldestFirst) {
        if (researchOldestFirst == null) return false;
        boolean changed = false;
        for (ResearchKey key : researchOldestFirst) {
            if (key != null && oldestFirst.add(key)) changed = true;
        }
        return changed;
    }

    public int size() {
        return oldestFirst.size();
    }

    public List<ResearchKey> snapshotOldestFirst() {
        return Collections.unmodifiableList(new ArrayList<ResearchKey>(oldestFirst));
    }

    public List<ResearchKey> snapshotNewestFirst() {
        List<ResearchKey> all = new ArrayList<ResearchKey>(oldestFirst);
        Collections.reverse(all);
        return Collections.unmodifiableList(all);
    }
}
