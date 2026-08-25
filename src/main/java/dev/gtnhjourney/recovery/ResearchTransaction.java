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
    private final List<DeletionStateChange> deletionChanges;

    public ResearchTransaction(
        long id,
        long timestamp,
        String description,
        List<ResearchEntrySnapshot> added,
        List<ResearchEntrySnapshot> removed) {
        this(id, timestamp, description, added, removed, Collections.<DeletionStateChange>emptyList());
    }

    public ResearchTransaction(
        long id,
        long timestamp,
        String description,
        List<ResearchEntrySnapshot> added,
        List<ResearchEntrySnapshot> removed,
        List<DeletionStateChange> deletionChanges) {
        this.id = id;
        this.timestamp = timestamp;
        this.description = description == null ? "" : description;
        this.added = copyEntries(added);
        this.removed = copyEntries(removed);
        this.deletionChanges = copyDeletionChanges(deletionChanges);
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

    public List<DeletionStateChange> deletionChanges() {
        return deletionChanges;
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty() && deletionChanges.isEmpty();
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

    private static List<DeletionStateChange> copyDeletionChanges(List<DeletionStateChange> source) {
        List<DeletionStateChange> copy = new ArrayList<DeletionStateChange>();
        if (source != null) {
            for (DeletionStateChange change : source) {
                if (change != null) copy.add(new DeletionStateChange(change.deletionId(), change.activeAfterForward()));
            }
        }
        return Collections.unmodifiableList(copy);
    }
}
