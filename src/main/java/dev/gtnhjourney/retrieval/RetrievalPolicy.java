package dev.gtnhjourney.retrieval;

import dev.gtnhjourney.research.ResearchKey;
import dev.gtnhjourney.research.ResearchRegistry;

/** Server-authoritative policy shared by commands and NEI packet handlers. */
public final class RetrievalPolicy {

    private RetrievalPolicy() {}

    public static boolean canRetrieve(ResearchRegistry registry, ResearchKey key) {
        return registry != null && key != null && registry.contains(key);
    }

    public static int clampAmount(int requestedAmount, int maxStackSize) {
        int safeMax = Math.max(1, maxStackSize);
        int safeRequested = Math.max(1, requestedAmount);
        return Math.min(safeRequested, safeMax);
    }
}
