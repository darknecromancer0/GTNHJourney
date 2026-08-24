package dev.gtnhjourney.network;

import java.nio.charset.StandardCharsets;

import dev.gtnhjourney.research.ResearchKey;

/** Conservative client-sync budget for research states with unusually large NBT payloads. */
public final class ResearchSyncBudget {
    /** Keeps SimpleNetworkWrapper payloads well below the vanilla 32767-byte CustomPayload ceiling, leaving room for FML framing. */
    public static final int TARGET_CHUNK_BYTES = 24 * 1024;
    public static final int MAX_ENTRIES_PER_CHUNK = 32;
    public static final int MAX_SINGLE_ENTRY_BYTES = 24 * 1024;

    private ResearchSyncBudget() {}

    public static int estimateBytes(ResearchKey key) {
        if (key == null) return Integer.MAX_VALUE;
        long bytes = 256L;
        bytes += utf8Length(key.getItemId());
        bytes += utf8Length(key.getCanonicalNbt());
        return bytes >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
    }

    public static boolean canSync(ResearchKey key) {
        return estimateBytes(key) <= MAX_SINGLE_ENTRY_BYTES;
    }


    public static boolean shouldFlush(int currentEntries, int currentEstimatedBytes, int nextEstimatedBytes) {
        if (currentEntries <= 0) return false;
        if (currentEntries >= MAX_ENTRIES_PER_CHUNK) return true;
        return ((long) currentEstimatedBytes + (long) nextEstimatedBytes) > TARGET_CHUNK_BYTES;
    }

    private static int utf8Length(String value) {
        if (value == null || value.isEmpty()) return 0;
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
