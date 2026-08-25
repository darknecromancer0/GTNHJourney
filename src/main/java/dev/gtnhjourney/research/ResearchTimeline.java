package dev.gtnhjourney.research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Preserves the order in which unique research states were first discovered. */
public final class ResearchTimeline {

    private final LinkedHashSet<ResearchKey> oldestFirst = new LinkedHashSet<ResearchKey>();

    /** @return true only when the key was not already present in the timeline. */
    public boolean record(ResearchKey key) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        return oldestFirst.add(key);
    }

    /** Reinserts a missing key at an exact recovery position, clamped to the current list bounds. */
    public boolean insertAt(ResearchKey key, int index) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        if (oldestFirst.contains(key)) return false;
        List<ResearchKey> ordered = new ArrayList<ResearchKey>(oldestFirst);
        int insertion = Math.max(0, Math.min(index, ordered.size()));
        ordered.add(insertion, key);
        restore(ordered);
        return true;
    }

    public void restore(Collection<ResearchKey> keys) {
        oldestFirst.clear();
        if (keys == null) return;
        for (ResearchKey key : keys) if (key != null) oldestFirst.add(key);
    }

    public boolean contains(ResearchKey key) {
        return key != null && oldestFirst.contains(key);
    }

    public boolean remove(ResearchKey key) {
        return key != null && oldestFirst.remove(key);
    }

    public void clear() {
        oldestFirst.clear();
    }

    public int size() {
        return oldestFirst.size();
    }

    public List<ResearchKey> snapshotOldestFirst() {
        return Collections.unmodifiableList(new ArrayList<ResearchKey>(oldestFirst));
    }

    public List<ResearchKey> snapshotNewest(int limit) {
        if (limit <= 0 || oldestFirst.isEmpty()) return Collections.emptyList();
        List<ResearchKey> all = new ArrayList<ResearchKey>(oldestFirst);
        List<ResearchKey> out = new ArrayList<ResearchKey>(Math.min(limit, all.size()));
        for (int i = all.size() - 1; i >= 0 && out.size() < limit; i--) out.add(all.get(i));
        return Collections.unmodifiableList(out);
    }
}
