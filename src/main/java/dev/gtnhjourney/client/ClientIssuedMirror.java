package dev.gtnhjourney.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import dev.gtnhjourney.research.ResearchFingerprint;

/** Client mirror for successful issuance chronology. Unlike research activity, this may contain native C-only items. */
public final class ClientIssuedMirror {

    private static final LinkedHashSet<ResearchFingerprint> oldestFirst = new LinkedHashSet<ResearchFingerprint>();
    private static final Map<ResearchFingerprint, Long> sequences = new HashMap<ResearchFingerprint, Long>();
    private static final List<ResearchFingerprint> staging = new ArrayList<ResearchFingerprint>();
    private static int epoch;
    private static boolean syncing;
    private static int expectedEntries = -1;
    private static long sequenceCounter;
    private static long revision;

    private ClientIssuedMirror() {}

    public static synchronized void begin(int newEpoch, int expectedIssuedEntries) {
        epoch = newEpoch;
        syncing = true;
        expectedEntries = expectedIssuedEntries < 0 ? -1 : expectedIssuedEntries;
        staging.clear();
    }

    public static synchronized void addChunk(int chunkEpoch, Iterable<ResearchFingerprint> fingerprints) {
        if (!syncing || chunkEpoch != epoch || fingerprints == null) return;
        for (ResearchFingerprint fingerprint : fingerprints) if (fingerprint != null) staging.add(fingerprint);
    }

    public static synchronized boolean isComplete(int finishEpoch) {
        return syncing && finishEpoch == epoch && (expectedEntries < 0 || staging.size() == expectedEntries);
    }

    public static synchronized void abort(int abortEpoch) {
        if (!syncing || abortEpoch != epoch) return;
        staging.clear();
        syncing = false;
        expectedEntries = -1;
    }

    public static synchronized void finish(int finishEpoch) {
        if (!isComplete(finishEpoch)) return;
        LinkedHashSet<ResearchFingerprint> next = new LinkedHashSet<ResearchFingerprint>();
        for (ResearchFingerprint fingerprint : staging) if (fingerprint != null) next.add(fingerprint);
        if (!next.equals(oldestFirst)) {
            oldestFirst.clear();
            oldestFirst.addAll(next);
            rebuildSequences();
            revision++;
        }
        staging.clear();
        syncing = false;
        expectedEntries = -1;
    }

    public static synchronized void touch(ResearchFingerprint fingerprint) {
        if (fingerprint == null) return;
        ResearchFingerprint newest = null;
        for (ResearchFingerprint candidate : oldestFirst) newest = candidate;
        if (fingerprint.equals(newest)) return;
        oldestFirst.remove(fingerprint);
        oldestFirst.add(fingerprint);
        sequences.put(fingerprint, Long.valueOf(++sequenceCounter));
        revision++;
    }

    public static synchronized long sequence(ResearchFingerprint fingerprint) {
        Long value = fingerprint == null ? null : sequences.get(fingerprint);
        return value == null ? -1L : value.longValue();
    }

    public static synchronized List<ResearchFingerprint> snapshotOldestFirst() {
        return Collections.unmodifiableList(new ArrayList<ResearchFingerprint>(oldestFirst));
    }

    public static synchronized long revision() { return revision; }

    public static synchronized void clear() {
        epoch++;
        syncing = false;
        expectedEntries = -1;
        staging.clear();
        if (!oldestFirst.isEmpty() || !sequences.isEmpty()) {
            oldestFirst.clear();
            sequences.clear();
            sequenceCounter = 0L;
            revision++;
        }
    }

    private static void rebuildSequences() {
        sequences.clear();
        sequenceCounter = 0L;
        for (ResearchFingerprint fingerprint : oldestFirst) {
            if (fingerprint != null) sequences.put(fingerprint, Long.valueOf(++sequenceCounter));
        }
    }
}
