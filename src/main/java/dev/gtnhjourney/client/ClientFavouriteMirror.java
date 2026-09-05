package dev.gtnhjourney.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dev.gtnhjourney.research.ResearchFingerprint;

/** Client-only exact favourite fingerprints received from the authoritative server. */
public final class ClientFavouriteMirror {

    private static volatile Set<ResearchFingerprint> values = Collections.emptySet();
    private static volatile long revision;

    private ClientFavouriteMirror() {}

    public static synchronized void replace(Collection<ResearchFingerprint> next) {
        Set<ResearchFingerprint> copy = new HashSet<ResearchFingerprint>();
        if (next != null) for (ResearchFingerprint value : next) if (value != null) copy.add(value);
        values = Collections.unmodifiableSet(copy);
        revision++;
    }

    public static boolean contains(ResearchFingerprint fingerprint) {
        return fingerprint != null && values.contains(fingerprint);
    }

    public static int size() { return values.size(); }
    public static long revision() { return revision; }

    public static synchronized void clear() {
        if (values.isEmpty()) return;
        values = Collections.emptySet();
        revision++;
    }
}
