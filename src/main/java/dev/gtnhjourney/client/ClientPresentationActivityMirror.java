package dev.gtnhjourney.client;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.gtnhjourney.research.ResearchKey;

/** Session-only activity for native C/NEI identities that are not necessarily researched. */
public final class ClientPresentationActivityMirror {

    private static final int MAX_ENTRIES = 8192;
    private static final LinkedHashMap<ResearchKey, Long> sequenceByKey = new LinkedHashMap<ResearchKey, Long>();
    private static long nextSequence;
    private static long revision;

    private ClientPresentationActivityMirror() {}

    public static synchronized void touch(ResearchKey key) {
        if (key == null) return;
        sequenceByKey.remove(key);
        sequenceByKey.put(key, Long.valueOf(++nextSequence));
        while (sequenceByKey.size() > MAX_ENTRIES) {
            ResearchKey oldest = sequenceByKey.keySet().iterator().next();
            sequenceByKey.remove(oldest);
        }
        revision++;
    }

    public static synchronized long sequence(ResearchKey key) {
        Long value = key == null ? null : sequenceByKey.get(key);
        return value == null ? -1L : value.longValue();
    }

    public static synchronized long revision() { return revision; }

    public static synchronized void clear() {
        if (sequenceByKey.isEmpty() && nextSequence == 0L) return;
        sequenceByKey.clear();
        nextSequence = 0L;
        revision++;
    }

    static synchronized int sizeForTests() { return sequenceByKey.size(); }
}
