package dev.gtnhjourney.recovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.minecraft.NbtCanonicalizer;
import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.research.ResearchKey;

/** Pure transaction engine used by the runtime mutation facade and recovery tests. */
public final class ResearchMutationEngine {

    private static final AtomicLong NEXT_ID = new AtomicLong(System.currentTimeMillis() * 1000L);
    private static final RecoveryRestorePolicy PERMISSIVE_RESTORE_POLICY = new RecoveryRestorePolicy() {

        @Override
        public boolean canRestore(ResearchEntrySnapshot entry) {
            return entry != null;
        }
    };

    private final JourneyResearchData research;
    private final JourneyRecoveryData recovery;
    private final UUID playerId;
    private final RecoveryRestorePolicy restorePolicy;

    public ResearchMutationEngine(JourneyResearchData research, JourneyRecoveryData recovery, UUID playerId) {
        this(research, recovery, playerId, PERMISSIVE_RESTORE_POLICY);
    }

    public ResearchMutationEngine(
        JourneyResearchData research,
        JourneyRecoveryData recovery,
        UUID playerId,
        RecoveryRestorePolicy restorePolicy) {
        if (research == null) throw new IllegalArgumentException("research must not be null");
        if (recovery == null) throw new IllegalArgumentException("recovery must not be null");
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        if (restorePolicy == null) throw new IllegalArgumentException("restorePolicy must not be null");
        this.research = research;
        this.recovery = recovery;
        this.playerId = playerId;
        this.restorePolicy = restorePolicy;
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

    /** Removes many exact states but records the whole destructive action as one reversible transaction. */
    public int deleteKeys(List<ResearchKey> keys, String description) {
        if (keys == null || keys.isEmpty()) return 0;

        ResearchStateSnapshot before = research.captureState(playerId);
        Map<ResearchKey, ResearchEntrySnapshot> original = new LinkedHashMap<ResearchKey, ResearchEntrySnapshot>();
        for (ResearchEntrySnapshot entry : before.entries()) original.put(entry.key(), entry);

        Set<ResearchKey> unique = new LinkedHashSet<ResearchKey>();
        for (ResearchKey key : keys) if (key != null) unique.add(key);
        List<ResearchEntrySnapshot> removed = new ArrayList<ResearchEntrySnapshot>();
        List<DeletionStateChange> changes = new ArrayList<DeletionStateChange>();
        long timestamp = System.currentTimeMillis();

        for (ResearchKey key : unique) {
            ResearchEntrySnapshot snapshot = original.get(key);
            if (snapshot == null) continue;
            if (research.removeEntry(playerId, key) == null) continue;
            long deletionId = nextId();
            recovery.appendDeletion(playerId, new DeletionRecord(deletionId, timestamp, snapshot, true));
            removed.add(snapshot);
            changes.add(new DeletionStateChange(deletionId, true));
        }
        if (removed.isEmpty()) return 0;

        record(
            new ResearchTransaction(
                nextId(),
                timestamp,
                description,
                Collections.<ResearchEntrySnapshot>emptyList(),
                removed,
                changes));
        return removed.size();
    }

    public int addEntries(List<ResearchEntrySnapshot> entries, String description) {
        if (entries == null || entries.isEmpty()) return 0;
        List<ResearchEntrySnapshot> ordered = sorted(entries);
        List<ResearchEntrySnapshot> added = new ArrayList<ResearchEntrySnapshot>();
        for (ResearchEntrySnapshot entry : ordered) {
            if (entry == null || research.registry(playerId).contains(entry.key())) continue;
            if (!restorePolicy.canRestore(entry)) continue;
            if (research.restoreEntry(playerId, entry)) added.add(entry);
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

    /** Replaces the complete authoritative state and records the replacement as one reversible transaction. */
    public int replaceState(ResearchStateSnapshot target, String description) {
        return replaceState(target, description, Collections.<DeletionStateChange>emptyList());
    }

    /** Replaces research and applies related deletion-history state changes in the same reversible transaction. */
    public int replaceState(
        ResearchStateSnapshot target,
        String description,
        List<DeletionStateChange> deletionChanges) {
        if (target == null || !canRestoreAll(target.entries())) return 0;
        ResearchStateSnapshot before = research.captureState(playerId);
        if (sameState(before, target)) return 0;

        removeEntries(before.entries());
        restoreEntries(target.entries());
        ResearchStateSnapshot applied = research.captureState(playerId);
        if (!sameState(applied, target)) {
            removeEntries(applied.entries());
            restoreEntries(before.entries());
            return 0;
        }

        applyDeletionChanges(deletionChanges, true);
        record(
            new ResearchTransaction(
                nextId(),
                System.currentTimeMillis(),
                description,
                target.entries(),
                before.entries(),
                deletionChanges));
        return target.size();
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
            if (!restorePolicy.canRestore(entry)) continue;
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
            if (!canRestoreAll(transaction.removed())
                || !canApplyDelta(transaction.added(), transaction.removed())) {
                recovery.pushUndo(playerId, transaction);
                break;
            }
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
            if (!canRestoreAll(transaction.added())) {
                recovery.pushRedo(playerId, transaction);
                break;
            }
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

    /**
     * Rejects a recovery delta before mutation when an existing same-key state no longer matches the snapshot that
     * this transaction expects to remove or restore. This keeps passive reacquisition from being silently overwritten
     * by an older explicit undo while still allowing already-desired exact states to make the operation idempotent.
     */
    private boolean canApplyDelta(
        List<ResearchEntrySnapshot> removals,
        List<ResearchEntrySnapshot> restorations) {
        Map<ResearchKey, ResearchEntrySnapshot> current = new LinkedHashMap<ResearchKey, ResearchEntrySnapshot>();
        for (ResearchEntrySnapshot entry : research.captureState(playerId).entries()) current.put(entry.key(), entry);

        Set<ResearchKey> removalKeys = new LinkedHashSet<ResearchKey>();
        if (removals != null) {
            for (ResearchEntrySnapshot removal : removals) {
                if (removal == null) continue;
                removalKeys.add(removal.key());
                ResearchEntrySnapshot existing = current.get(removal.key());
                if (existing != null && !sameEntry(existing, removal)) return false;
            }
        }

        if (restorations != null) {
            for (ResearchEntrySnapshot restoration : restorations) {
                if (restoration == null) continue;
                ResearchEntrySnapshot existing = current.get(restoration.key());
                if (existing == null || removalKeys.contains(restoration.key())) continue;
                if (!sameEntry(existing, restoration)) return false;
            }
        }
        return true;
    }

    private static boolean sameEntry(ResearchEntrySnapshot left, ResearchEntrySnapshot right) {
        return left != null && right != null && left.key().equals(right.key())
            && left.timelineIndex() == right.timelineIndex() && sameTemplate(left.template(), right.template());
    }

    private boolean canRestoreAll(List<ResearchEntrySnapshot> entries) {
        if (entries == null) return true;
        for (ResearchEntrySnapshot entry : entries) {
            if (entry != null && !restorePolicy.canRestore(entry)) return false;
        }
        return true;
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

    static boolean sameState(ResearchStateSnapshot left, ResearchStateSnapshot right) {
        if (left == right) return true;
        if (left == null || right == null || left.size() != right.size()) return false;
        List<ResearchEntrySnapshot> leftEntries = left.entries();
        List<ResearchEntrySnapshot> rightEntries = right.entries();
        for (int i = 0; i < leftEntries.size(); i++) {
            ResearchEntrySnapshot a = leftEntries.get(i);
            ResearchEntrySnapshot b = rightEntries.get(i);
            if (!a.key().equals(b.key()) || a.timelineIndex() != b.timelineIndex()) return false;
            if (!sameTemplate(a.template(), b.template())) return false;
        }
        return true;
    }

    private static boolean sameTemplate(NBTTagCompound left, NBTTagCompound right) {
        try {
            return NbtCanonicalizer.canonicalize(left).equals(NbtCanonicalizer.canonicalize(right));
        } catch (IllegalArgumentException unsafe) {
            return false;
        } catch (RuntimeException unsafe) {
            return false;
        }
    }

    private static int clampCount(int count) {
        return Math.max(1, Math.min(100, count));
    }

    private static long nextId() {
        return NEXT_ID.incrementAndGet();
    }
}
