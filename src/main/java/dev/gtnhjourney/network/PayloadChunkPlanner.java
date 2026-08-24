package dev.gtnhjourney.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure deterministic planner for payload-sized chunks. Values are serialized entry sizes in bytes; null/negative
 * sizes fail closed as unsyncable. Returned chunks contain source indices so callers can keep their own objects.
 */
public final class PayloadChunkPlanner {

    private PayloadChunkPlanner() {}

    public static Plan plan(List<Integer> entrySizes, int maxEntriesPerChunk, int targetChunkBytes,
        int maxSingleEntryBytes) {
        if (maxEntriesPerChunk < 1) throw new IllegalArgumentException("maxEntriesPerChunk must be positive");
        if (targetChunkBytes < 1) throw new IllegalArgumentException("targetChunkBytes must be positive");
        if (maxSingleEntryBytes < 0) throw new IllegalArgumentException("maxSingleEntryBytes must not be negative");
        if (entrySizes == null || entrySizes.isEmpty()) {
            return new Plan(0, 0, 0, Collections.<List<Integer>>emptyList());
        }

        List<List<Integer>> chunks = new ArrayList<List<Integer>>();
        List<Integer> current = new ArrayList<Integer>();
        int currentBytes = 0;
        int syncable = 0;
        int oversized = 0;

        for (int i = 0; i < entrySizes.size(); i++) {
            Integer boxed = entrySizes.get(i);
            if (boxed == null || boxed.intValue() < 0 || boxed.intValue() > maxSingleEntryBytes) {
                oversized++;
                continue;
            }
            int bytes = boxed.intValue();
            if (!current.isEmpty()
                && (current.size() >= maxEntriesPerChunk || (long) currentBytes + (long) bytes > targetChunkBytes)) {
                chunks.add(Collections.unmodifiableList(current));
                current = new ArrayList<Integer>();
                currentBytes = 0;
            }
            current.add(Integer.valueOf(i));
            currentBytes += bytes;
            syncable++;
        }
        if (!current.isEmpty()) chunks.add(Collections.unmodifiableList(current));
        return new Plan(entrySizes.size(), syncable, oversized, Collections.unmodifiableList(chunks));
    }

    public static final class Plan {

        private final int sourceTotal;
        private final int syncableTotal;
        private final int oversizedTotal;
        private final List<List<Integer>> chunks;

        private Plan(int sourceTotal, int syncableTotal, int oversizedTotal, List<List<Integer>> chunks) {
            this.sourceTotal = sourceTotal;
            this.syncableTotal = syncableTotal;
            this.oversizedTotal = oversizedTotal;
            this.chunks = chunks;
        }

        public int getSourceTotal() {
            return sourceTotal;
        }

        public int getSyncableTotal() {
            return syncableTotal;
        }

        public int getOversizedTotal() {
            return oversizedTotal;
        }

        public List<List<Integer>> getChunks() {
            return chunks;
        }
    }
}
