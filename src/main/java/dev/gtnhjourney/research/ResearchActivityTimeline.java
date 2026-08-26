package dev.gtnhjourney.research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Orders researched states by meaningful Journey activity. A genuine new-research event and a successful Journey
 * retrieval both touch the state to newest. Re-observing an already researched item never calls recordUnlock.
 */
public final class ResearchActivityTimeline {

    private final LinkedHashSet<ResearchKey> oldestFirst = new LinkedHashSet<ResearchKey>();

    /** Called only for a state the authoritative research registry has just reported as newly added. */
    public boolean recordUnlock(ResearchKey key) {
        return touch(key);
    }

    public boolean recordRetrieval(ResearchKey key) {
        return touch(key);
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

    private boolean touch(ResearchKey key) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        ResearchKey newest = null;
        for (ResearchKey candidate : oldestFirst) newest = candidate;
        if (key.equals(newest)) return false;
        oldestFirst.remove(key);
        oldestFirst.add(key);
        return true;
    }
}
