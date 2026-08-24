package dev.gtnhjourney.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded in-memory ledger of third-party observation failures for first-run diagnostics. */
public final class ResearchFailureLog {
    public static final int MAX_UNIQUE = 128;
    private static final Map<String, MutableEntry> ENTRIES = new LinkedHashMap<String, MutableEntry>();
    private static int droppedUnique;

    private ResearchFailureLog() {}

    /** @return true only for the first occurrence of a unique item+failure pair. */
    public static synchronized boolean record(String item, String failure) {
        String safeItem = item == null || item.isEmpty() ? "<unknown-item>" : item;
        String safeFailure = failure == null || failure.isEmpty() ? "<unknown-failure>" : failure;
        String key = safeItem + '\n' + safeFailure;
        MutableEntry existing = ENTRIES.get(key);
        if (existing != null) {
            existing.occurrences++;
            return false;
        }
        if (ENTRIES.size() >= MAX_UNIQUE) {
            droppedUnique++;
            return false;
        }
        ENTRIES.put(key, new MutableEntry(safeItem, safeFailure));
        return true;
    }

    public static synchronized List<Entry> snapshot() {
        List<Entry> out = new ArrayList<Entry>(ENTRIES.size());
        for (MutableEntry entry : ENTRIES.values()) {
            out.add(new Entry(entry.item, entry.failure, entry.occurrences));
        }
        return Collections.unmodifiableList(out);
    }

    public static synchronized int droppedUnique() { return droppedUnique; }
    public static synchronized int uniqueCount() { return ENTRIES.size(); }

    public static synchronized void clear() {
        ENTRIES.clear();
        droppedUnique = 0;
    }

    private static final class MutableEntry {
        final String item;
        final String failure;
        int occurrences = 1;
        MutableEntry(String item, String failure) { this.item = item; this.failure = failure; }
    }

    public static final class Entry {
        private final String item;
        private final String failure;
        private final int occurrences;
        Entry(String item, String failure, int occurrences) {
            this.item = item;
            this.failure = failure;
            this.occurrences = occurrences;
        }
        public String getItem() { return item; }
        public String getFailure() { return failure; }
        public int getOccurrences() { return occurrences; }
    }
}
