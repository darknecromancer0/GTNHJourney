package dev.gtnhjourney.recovery;

/** Immutable persistent record of one explicit research deletion. */
public final class DeletionRecord {

    private final long id;
    private final long timestamp;
    private final ResearchEntrySnapshot entry;
    private final boolean active;

    public DeletionRecord(long id, long timestamp, ResearchEntrySnapshot entry, boolean active) {
        if (entry == null) throw new IllegalArgumentException("entry must not be null");
        this.id = id;
        this.timestamp = timestamp;
        this.entry = new ResearchEntrySnapshot(entry.key(), entry.template(), entry.timelineIndex());
        this.active = active;
    }

    public long id() {
        return id;
    }

    public long timestamp() {
        return timestamp;
    }

    public ResearchEntrySnapshot entry() {
        return new ResearchEntrySnapshot(entry.key(), entry.template(), entry.timelineIndex());
    }

    public boolean active() {
        return active;
    }

    public DeletionRecord withActive(boolean value) {
        return value == active ? this : new DeletionRecord(id, timestamp, entry, value);
    }
}
