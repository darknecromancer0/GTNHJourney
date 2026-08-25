package dev.gtnhjourney.recovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.research.ResearchKey;

/** Pure transaction engine used by the runtime mutation facade and recovery tests. */
public final class ResearchMutationEngine {

    private static final AtomicLong NEXT_ID = new AtomicLong(System.currentTimeMillis() * 1000L);

    private final JourneyResearchData research;
    private final JourneyRecoveryData recovery;
    private final UUID playerId;

    public ResearchMutationEngine(JourneyResearchData research, JourneyRecoveryData recovery, UUID playerId) {
        if (research == null) throw new IllegalArgumentException("research must not be null");
        if (recovery == null) throw new IllegalArgumentException("recovery must not be null");
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        this.research = research;
        this.recovery = recovery;
        this.playerId = playerId;
    }

    public boolean deleteExact(ResearchKey key, String description) {
        ResearchEntrySnapshot removed = research.removeEntry(playerId, key);
        if (removed == null) return false;
        long id = nextId();
        long timestamp = System.currentTimeMillis();
        recovery.appendDeletion(playerId, new DeletionRecord(id, timestamp, removed, true));
        record(
            new ResearchTransaction(
                id,
                timestamp,
                description,
                Collections.<ResearchEntrySnapshot>emptyList(),
                Collections.singletonList(removed),
                Collections.singletonList(new DeletionStateChange(id, true))));
        return true;
    }

    public int addEntries(List<ResearchEntrySnapshot> entries, String description) {
        if (entries == null || entries.isEmpty()) return 0;
        List<ResearchEntrySnapshot> ordered = sorted(entries);
        List<ResearchEntrySnapshot> added = new ArrayList<ResearchEntrySnapshot>();
        for (ResearchEntrySnapshot entry : ordered) {
            if (entry != null && research.restoreEntry(playerId, entry)) added.add(entry);
        }
        if (added.isEmpty()) return 0;
        record(
            new ResearchTransaction(
                nextId(),
                System.currentTimeMillis(),
                description,
                added,
                Collections.<ResearchEntrySnapshot>emptyList()));
        return added.size();
    }

    /** Records a delta that has already been applied authoritatively, for example by semantic observation expansion. */
    public boolean recordApplied(
        List<ResearchEntrySnapshot> added,
        List<ResearchEntrySnapshot> removed,
        String description) {
        ResearchTransaction transaction = new ResearchTransaction(
            nextId(),
            System.currentTimeMillis(),
            description,
            added,
            removed);
        if (transaction.isEmpty()) return false;
        record(transaction);
        return true;
    }

    public int restoreDeleted(int count) {
        List<DeletionRecord> records = recovery.newestActiveDeletions(playerId, Math.max(1, Math.min(1000, count)));
        if (records.isEmpty()) return 0;
        List<ResearchEntrySnapshot> restored = new ArrayList<ResearchEntrySnapshot>();
        List<DeletionStateChange> changes = new ArrayList<DeletionStateChange>();
        for (DeletionRecord record : records) {
            ResearchEntrySnapshot entry = record.entry();
            if (research.registry(playerId).contains(entry.key())) {
                recovery.setDeletionActive(playerId, record.id(), false);
                continue;
            }
            if (!research.restoreEntry(playerId, entry)) continue;
            recovery.setDeletionActive(playerId, record.id(), false);
            restored.add(entry);
            changes.add(new DeletionStateChange(record.id(), false));
        }
        if (restored.isEmpty()) return 0;
        record(
            new ResearchTransaction(
                nextId(),
                System.currentTimeMillis(),
                "Restore deleted " + restored.size(),
                restored,
                Collections.<ResearchEntrySnapshot>emptyList(),
                changes));
        return restored.size();
    }

    public int undo(int count) {
        int requested = clampCount(count);
        int applied = 0;
        while (applied < requested) {
            ResearchTransaction transaction = recovery.popUndo(playerId);
            if (transaction == null) break;
            applyReverse(transaction);
            recovery.pushRedo(playerId, transaction);
            applied++;
        }
        return applied;
    }

    public int redo(int count) {
        int requested = clampCount(count);
        int applied = 0;
        while (applied < requested) {
            ResearchTransaction transaction = recovery.popRedo(playerId);
            if (transaction == null) break;
            applyForward(transaction);
            recovery.pushUndo(playerId, transaction);
            applied++;
        }
        return applied;
    }

    public void notePassiveMutation() {
        recovery.clearRedo(playerId);
    }

    public void notePassivePresent(ResearchKey key) {
        recovery.clearRedo(playerId);
        if (key != null) recovery.markDeletionInactiveForPresentKey(playerId, key);
    }

    private void record(ResearchTransaction transaction) {
        if (transaction == null || transaction.isEmpty()) return;
        recovery.clearRedo(playerId);
        recovery.pushUndo(playerId, transaction);
    }

    private void applyForward(ResearchTransaction transaction) {
        removeEntries(transaction.removed());
        restoreEntries(transaction.added());
        applyDeletionChanges(transaction.deletionChanges(), true);
    }

    private void applyReverse(ResearchTransaction transaction) {
        removeEntries(transaction.added());
        restoreEntries(transaction.removed());
        applyDeletionChanges(transaction.deletionChanges(), false);
    }

    private void applyDeletionChanges(List<DeletionStateChange> changes, boolean forward) {
        if (changes == null) return;
        for (DeletionStateChange change : changes) {
            if (change == null) continue;
            boolean active = forward ? change.activeAfterForward() : !change.activeAfterForward();
            recovery.setDeletionActive(playerId, change.deletionId(), active);
        }
    }

    private void removeEntries(List<ResearchEntrySnapshot> entries) {
        if (entries == null) return;
        for (ResearchEntrySnapshot entry : entries) {
            if (entry != null) research.removeEntry(playerId, entry.key());
        }
    }

    private void restoreEntries(List<ResearchEntrySnapshot> entries) {
        for (ResearchEntrySnapshot entry : sorted(entries)) research.restoreEntry(playerId, entry);
    }

    private static List<ResearchEntrySnapshot> sorted(List<ResearchEntrySnapshot> entries) {
        List<ResearchEntrySnapshot> ordered = new ArrayList<ResearchEntrySnapshot>();
        if (entries != null) for (ResearchEntrySnapshot entry : entries) if (entry != null) ordered.add(entry);
        Collections.sort(ordered, new Comparator<ResearchEntrySnapshot>() {

            @Override
            public int compare(ResearchEntrySnapshot left, ResearchEntrySnapshot right) {
                return Integer.compare(left.timelineIndex(), right.timelineIndex());
            }
        });
        return ordered;
    }

    private static int clampCount(int count) {
        return Math.max(1, Math.min(100, count));
    }

    private static long nextId() {
        return NEXT_ID.incrementAndGet();
    }
}
