package dev.gtnhjourney.research;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

/** Owns the researched item keys for one logical player scope. */
public final class ResearchRegistry {

    private final NavigableSet<ResearchKey> researched = new TreeSet<ResearchKey>();
    private final Map<ResearchFingerprint, ResearchKey> byFingerprint = new HashMap<ResearchFingerprint, ResearchKey>();

    /**
     * @return true only when this call added a previously unknown key.
     */
    public boolean unlock(ResearchKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        boolean added = researched.add(key);
        if (added) byFingerprint.put(ResearchFingerprint.of(key), key);
        return added;
    }

    public boolean contains(ResearchKey key) {
        return key != null && researched.contains(key);
    }

    public int size() {
        return researched.size();
    }

    public ResearchKey find(ResearchFingerprint fingerprint) {
        return fingerprint == null ? null : byFingerprint.get(fingerprint);
    }

    public boolean remove(ResearchKey key) {
        if (key == null || !researched.remove(key)) return false;
        byFingerprint.remove(ResearchFingerprint.of(key));
        return true;
    }

    public void replaceAll(Collection<ResearchKey> keys) {
        researched.clear();
        byFingerprint.clear();
        if (keys == null) return;
        for (ResearchKey key : keys) {
            if (key != null && researched.add(key)) byFingerprint.put(ResearchFingerprint.of(key), key);
        }
    }

    public void clear() {
        researched.clear();
        byFingerprint.clear();
    }

    /** Returns an immutable, deterministic snapshot suitable for persistence and synchronization. */
    public List<ResearchKey> snapshot() {
        return Collections.unmodifiableList(new ArrayList<ResearchKey>(researched));
    }
}
