package dev.gtnhjourney.recovery;

/** Decides whether one persisted recovery entry can be reconstructed safely in the current runtime pack. */
public interface RecoveryRestorePolicy {

    boolean canRestore(ResearchEntrySnapshot entry);
}
