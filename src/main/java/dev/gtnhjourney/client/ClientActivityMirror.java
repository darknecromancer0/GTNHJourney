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
    private static int expectedEntries = -1;
    private static long revision;

    private ClientActivityMirror() {}

    /** Legacy/test entry point when no authoritative expected activity count is available. */
    public static synchronized void begin(int newEpoch) {
        begin(newEpoch, -1);
    }

    public static synchronized void begin(int newEpoch, int expectedActivityEntries) {
        epoch = newEpoch;
        syncing = true;
        expectedEntries = expectedActivityEntries < 0 ? -1 : expectedActivityEntries;
        staging.clear();
    }

    public static synchronized void addChunk(int chunkEpoch, Iterable<ResearchFingerprint> fingerprints) {
        if (!syncing || chunkEpoch != epoch || fingerprints == null) return;
        for (ResearchFingerprint fingerprint : fingerprints) if (fingerprint != null) staging.add(fingerprint);
    }

    /** True only when the matching staged epoch contains the complete server-declared activity membership. */
    public static synchronized boolean isComplete(int finishEpoch) {
        return syncing && finishEpoch == epoch && (expectedEntries < 0 || staging.size() == expectedEntries);
    }

    /** Discards only the matching staged epoch. A stale End must never cancel a newer sync already in flight. */
    public static synchronized void abort(int abortEpoch) {
        if (!syncing || abortEpoch != epoch) return;
        staging.clear();
        syncing = false;
        expectedEntries = -1;
    }

    public static synchronized void finish(int finishEpoch, Collection<ResearchKey> researchOldestFirst) {
        if (!isComplete(finishEpoch)) return;
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
        // Unknown/missing research keys may be server-only and therefore have no client template. Among visible keys,
        // genuinely missing activity records stay old rather than being promoted to recent.
        for (ResearchKey key : research) if (key != null && !activityKeys.contains(key)) next.add(key);
        next.addAll(resolvedActivity);

        if (!next.equals(oldestFirst)) {
            oldestFirst.clear();
            oldestFirst.addAll(next);
            revision++;
        }
        staging.clear();
        syncing = false;
        expectedEntries = -1;
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
        expectedEntries = -1;
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
