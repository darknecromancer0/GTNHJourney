package dev.gtnhjourney.recovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable delta describing one explicit Journey mutation. */
public final class ResearchTransaction {

    private final long id;
    private final long timestamp;
    private final String description;
    private final List<ResearchEntrySnapshot> added;
    private final List<ResearchEntrySnapshot> removed;

    public ResearchTransaction(
        long id,
        long timestamp,
        String description,
        List<ResearchEntrySnapshot> added,
        List<ResearchEntrySnapshot> removed) {
        this.id = id;
        this.timestamp = timestamp;
        this.description = description == null ? "" : description;
        this.added = copyEntries(added);
        this.removed = copyEntries(removed);
    }

    public long id() {
        return id;
    }

    public long timestamp() {
        return timestamp;
    }

    public String description() {
        return description;
    }

    public List<ResearchEntrySnapshot> added() {
        return added;
    }

    public List<ResearchEntrySnapshot> removed() {
        return removed;
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty();
    }

    private static List<ResearchEntrySnapshot> copyEntries(List<ResearchEntrySnapshot> source) {
        List<ResearchEntrySnapshot> copy = new ArrayList<ResearchEntrySnapshot>();
        if (source != null) {
            for (ResearchEntrySnapshot entry : source) {
                if (entry != null) {
                    copy.add(new ResearchEntrySnapshot(entry.key(), entry.template(), entry.timelineIndex()));
                }
            }
        }
        return Collections.unmodifiableList(copy);
    }
}
