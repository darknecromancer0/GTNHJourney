package dev.gtnhjourney.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.gtnhjourney.research.ResearchKey;

/** Pure, deterministic chunk planner shared by runtime sync and offline tests. */
public final class ResearchSyncPlanner {
    private ResearchSyncPlanner() {}

    /**
     * Plans sync chunks as source indices. Null/oversized keys are intentionally omitted while relative order is kept.
     */
    public static Plan plan(List<ResearchKey> keys) {
        if (keys == null || keys.isEmpty()) return new Plan(0, 0, 0, Collections.<List<Integer>>emptyList());
        List<Integer> sizes = new ArrayList<Integer>(keys.size());
        for (ResearchKey key : keys) {
            sizes.add(key == null ? null : Integer.valueOf(ResearchSyncBudget.estimateBytes(key)));
        }
        PayloadChunkPlanner.Plan generic = PayloadChunkPlanner.plan(
            sizes,
            ResearchSyncBudget.MAX_ENTRIES_PER_CHUNK,
            ResearchSyncBudget.TARGET_CHUNK_BYTES,
            ResearchSyncBudget.MAX_SINGLE_ENTRY_BYTES);
        return new Plan(
            generic.getSourceTotal(),
            generic.getSyncableTotal(),
            generic.getOversizedTotal(),
            generic.getChunks());
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

        public int getSourceTotal() { return sourceTotal; }
        public int getSyncableTotal() { return syncableTotal; }
        public int getOversizedTotal() { return oversizedTotal; }
        public List<List<Integer>> getChunks() { return chunks; }
    }
}
