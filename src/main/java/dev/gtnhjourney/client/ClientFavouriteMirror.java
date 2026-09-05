package dev.gtnhjourney.client;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.gtnhjourney.research.ResearchFingerprint;

/** Client-only exact favourite fingerprints plus authoritative added chronology. */
public final class ClientFavouriteMirror {

    private static volatile Map<ResearchFingerprint, Long> values = Collections.emptyMap();
    private static volatile long revision;

    private ClientFavouriteMirror() {}

    /** Backward-compatible replacement for tests/old callers without explicit chronology. */
    public static synchronized void replace(Collection<ResearchFingerprint> next) {
        LinkedHashMap<ResearchFingerprint, Long> copy = new LinkedHashMap<ResearchFingerprint, Long>();
        long sequence = 0L;
        if (next != null) {
            for (ResearchFingerprint value : next) if (value != null && !copy.containsKey(value)) {
                copy.put(value, Long.valueOf(++sequence));
            }
        }
        replaceEntries(copy);
    }

    public static synchronized void replaceEntries(Map<ResearchFingerprint, Long> next) {
        LinkedHashMap<ResearchFingerprint, Long> copy = new LinkedHashMap<ResearchFingerprint, Long>();
        if (next != null) {
            for (Map.Entry<ResearchFingerprint, Long> entry : next.entrySet()) {
                if (entry.getKey() == null) continue;
                long sequence = entry.getValue() == null ? -1L : entry.getValue().longValue();
                copy.put(entry.getKey(), Long.valueOf(sequence));
            }
        }
        Map<ResearchFingerprint, Long> immutable = Collections.unmodifiableMap(copy);
        if (!immutable.equals(values)) {
            values = immutable;
            revision++;
        }
    }

    public static boolean contains(ResearchFingerprint fingerprint) {
        return fingerprint != null && values.containsKey(fingerprint);
    }

    public static long addSequence(ResearchFingerprint fingerprint) {
        Long sequence = fingerprint == null ? null : values.get(fingerprint);
        return sequence == null ? -1L : sequence.longValue();
    }

    public static int size() { return values.size(); }
    public static long revision() { return revision; }

    public static synchronized void clear() {
        if (values.isEmpty()) return;
        values = Collections.emptyMap();
        revision++;
    }
}
