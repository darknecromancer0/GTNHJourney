package dev.gtnhjourney.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dev.gtnhjourney.research.ResearchKey;

/** Thread-safe client-side read-only mirror populated only by server sync packets. */
public final class ClientResearchMirror {

    private static volatile Set<ResearchKey> keys = Collections.emptySet();
    private static volatile long revision;

    private ClientResearchMirror() {}

    public static synchronized void replace(Collection<ResearchKey> replacement) {
        HashSet<ResearchKey> copy = new HashSet<ResearchKey>();
        if (replacement != null) {
            for (ResearchKey key : replacement) if (key != null) copy.add(key);
        }
        Set<ResearchKey> next = Collections.unmodifiableSet(copy);
        if (!next.equals(keys)) {
            keys = next;
            revision++;
        }
    }

    public static synchronized void add(ResearchKey key) {
        if (key == null || keys.contains(key)) return;
        HashSet<ResearchKey> copy = new HashSet<ResearchKey>(keys);
        copy.add(key);
        keys = Collections.unmodifiableSet(copy);
        revision++;
    }

    public static boolean contains(ResearchKey key) {
        return key != null && keys.contains(key);
    }

    public static Collection<ResearchKey> snapshot() {
        ArrayList<ResearchKey> copy = new ArrayList<ResearchKey>(keys);
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    public static synchronized void clear() {
        if (keys.isEmpty()) return;
        keys = Collections.emptySet();
        revision++;
    }

    public static long revision() {
        return revision;
    }
}
