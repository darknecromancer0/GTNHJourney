package dev.gtnhjourney.recovery;

/** Exact deletion-history activity state that belongs to one research transaction. */
public final class DeletionStateChange {

    private final long deletionId;
    private final boolean activeAfterForward;

    public DeletionStateChange(long deletionId, boolean activeAfterForward) {
        this.deletionId = deletionId;
        this.activeAfterForward = activeAfterForward;
    }

    public long deletionId() {
        return deletionId;
    }

    public boolean activeAfterForward() {
        return activeAfterForward;
    }
}
