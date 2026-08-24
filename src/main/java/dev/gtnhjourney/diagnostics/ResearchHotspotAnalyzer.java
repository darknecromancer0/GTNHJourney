package dev.gtnhjourney.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.gtnhjourney.research.ResearchKey;

/** Finds base item/meta variants that are producing unusually many semantic NBT research states. */
public final class ResearchHotspotAnalyzer {
    public static final class Hotspot {
        private final String baseId;
        private final int states;
        Hotspot(String baseId, int states) { this.baseId = baseId; this.states = states; }
        public String getBaseId() { return baseId; }
        public int getStates() { return states; }
    }

    private ResearchHotspotAnalyzer() {}

    public static List<Hotspot> top(Iterable<ResearchKey> keys, int limit) {
        if (keys == null || limit <= 0) return Collections.emptyList();
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (ResearchKey key : keys) {
            if (key == null) continue;
            String base = key.getItemId() + "@" + key.getMeta();
            Integer old = counts.get(base);
            counts.put(base, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
        }
        List<Hotspot> out = new ArrayList<Hotspot>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            out.add(new Hotspot(entry.getKey(), entry.getValue().intValue()));
        }
        Collections.sort(out, new Comparator<Hotspot>() {
            @Override public int compare(Hotspot a, Hotspot b) {
                int byCount = Integer.compare(b.states, a.states);
                return byCount != 0 ? byCount : a.baseId.compareTo(b.baseId);
            }
        });
        if (out.size() > limit) out = new ArrayList<Hotspot>(out.subList(0, limit));
        return Collections.unmodifiableList(out);
    }
}
