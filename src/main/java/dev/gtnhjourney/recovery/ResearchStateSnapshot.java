package dev.gtnhjourney.recovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable ordered snapshot of one player's authoritative Journey research state. */
public final class ResearchStateSnapshot {

    private final List<ResearchEntrySnapshot> entries;

    public ResearchStateSnapshot(List<ResearchEntrySnapshot> entries) {
        List<ResearchEntrySnapshot> copy = new ArrayList<ResearchEntrySnapshot>();
        if (entries != null) {
            for (ResearchEntrySnapshot entry : entries) if (entry != null) copy.add(entry);
        }
        this.entries = Collections.unmodifiableList(copy);
    }

    public List<ResearchEntrySnapshot> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
