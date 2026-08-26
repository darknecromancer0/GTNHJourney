package dev.gtnhjourney.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/** Client mirror for N ordering. This is chronology only; ClientStackMirror remains authoritative for item templates. */
public final class ClientActivityMirror {

    private static final LinkedHashSet<ResearchKey> oldestFirst = new LinkedHashSet<ResearchKey>();
    private static final List<ResearchFingerprint> staging = new ArrayList<ResearchFingerprint>();
    private static int epoch;
    private static boolean syncing;
    private static long revision;

    private ClientActivityMirror() {}

    public static synchronized void begin(int newEpoch) {
        epoch = newEpoch;
        syncing = true;
        staging.clear();
    }

    public static synchronized void addChunk(int chunkEpoch, Iterable<ResearchFingerprint> fingerprints) {
        if (!syncing || chunkEpoch != epoch || fingerprints == null) return;
        for (ResearchFingerprint fingerprint : fingerprints) if (fingerprint != null) staging.add(fingerprint);
    }

    /** Discards only the matching staged epoch. A stale End must never cancel a newer sync already in flight. */
    public static synchronized void abort(int abortEpoch) {
        if (!syncing || abortEpoch != epoch) return;
        staging.clear();
        syncing = false;
    }

    public static synchronized void finish(int finishEpoch, Collection<ResearchKey> researchOldestFirst) {
        if (!syncing || finishEpoch != epoch) return;
        List<ResearchKey> research = researchOldestFirst == null ? Collections.<ResearchKey>emptyList()
            : new ArrayList<ResearchKey>(researchOldestFirst);
        Map<ResearchFingerprint, ResearchKey> byFingerprint = new HashMap<ResearchFingerprint, ResearchKey>();
        for (ResearchKey key : research) if (key != null) byFingerprint.put(ResearchFingerprint.of(key), key);

        Set<ResearchKey> activityKeys = new HashSet<ResearchKey>();
        List<ResearchKey> resolvedActivity = new ArrayList<ResearchKey>();
        for (ResearchFingerprint fingerprint : staging) {
            ResearchKey key = byFingerprint.get(fingerprint);
            if (key != null && activityKeys.add(key)) resolvedActivity.add(key);
        }

        LinkedHashSet<ResearchKey> next = new LinkedHashSet<ResearchKey>();
        // Unknown/missing activity records are treated as old, never as recent. This also migrates old saves safely.
        for (ResearchKey key : research) if (key != null && !activityKeys.contains(key)) next.add(key);
        next.addAll(resolvedActivity);

        if (!next.equals(oldestFirst)) {
            oldestFirst.clear();
            oldestFirst.addAll(next);
            revision++;
        }
        staging.clear();
        syncing = false;
    }

    /** Called only when the server has reported an actually new researched state. */
    public static synchronized void recordUnlock(ResearchKey key) {
        touch(key);
    }

    public static synchronized void recordRetrieval(ResearchKey key) {
        touch(key);
    }

    public static synchronized void remove(ResearchKey key) {
        if (key != null && oldestFirst.remove(key)) revision++;
    }

    public static synchronized List<ResearchKey> snapshotOldestFirst() {
        return Collections.unmodifiableList(new ArrayList<ResearchKey>(oldestFirst));
    }

    public static synchronized long revision() {
        return revision;
    }

    public static synchronized void clear() {
        epoch++;
        syncing = false;
        staging.clear();
        if (!oldestFirst.isEmpty()) {
            oldestFirst.clear();
            revision++;
        }
    }

    private static void touch(ResearchKey key) {
        if (key == null) return;
        ResearchKey newest = null;
        for (ResearchKey candidate : oldestFirst) newest = candidate;
        if (key.equals(newest)) return;
        oldestFirst.remove(key);
        oldestFirst.add(key);
        revision++;
    }
}
