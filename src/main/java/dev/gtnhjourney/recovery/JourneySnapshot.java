package dev.gtnhjourney.recovery;

/** Immutable bounded recovery snapshot containing only authoritative research state. */
public final class JourneySnapshot {

    private final long id;
    private final String name;
    private final long worldTick;
    private final SnapshotKind kind;
    private final ResearchStateSnapshot state;

    public JourneySnapshot(long id, String name, long worldTick, SnapshotKind kind, ResearchStateSnapshot state) {
        if (kind == null) throw new IllegalArgumentException("kind must not be null");
        this.id = id;
        this.name = name == null ? "" : name;
        this.worldTick = worldTick;
        this.kind = kind;
        this.state = copyState(state);
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public long worldTick() {
        return worldTick;
    }

    public SnapshotKind kind() {
        return kind;
    }

    public ResearchStateSnapshot state() {
        return copyState(state);
    }

    private static ResearchStateSnapshot copyState(ResearchStateSnapshot source) {
        return source == null ? new ResearchStateSnapshot(null) : new ResearchStateSnapshot(source.entries());
    }
}
