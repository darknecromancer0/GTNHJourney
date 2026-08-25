package dev.gtnhjourney.recovery;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.minecraft.NbtCanonicalizer;
import dev.gtnhjourney.persistence.JourneySnapshotData;

/** Bounded snapshot policy shared by manual, automatic and pre-mutation safety snapshots. */
public final class JourneySnapshotService {

    public static final long AUTO_INTERVAL_TICKS = 2400L;

    private static final AtomicLong NEXT_ID = new AtomicLong(System.currentTimeMillis() * 1000L);

    private final JourneySnapshotData data;
    private long skippedSuspiciousSnapshots;

    public JourneySnapshotService(JourneySnapshotData data) {
        if (data == null) throw new IllegalArgumentException("data must not be null");
        this.data = data;
    }

    public boolean maybeAutoSnapshot(UUID playerId, long worldTick, boolean loaded, ResearchStateSnapshot current) {
        if (playerId == null || !loaded || current == null) return false;
        JourneySnapshot previousAuto = data.latestAuto(playerId);
        if (previousAuto == null) {
            if (worldTick < AUTO_INTERVAL_TICKS) return false;
        } else {
            if (worldTick < previousAuto.worldTick() || worldTick - previousAuto.worldTick() < AUTO_INTERVAL_TICKS) {
                return false;
            }
            if (sameState(previousAuto.state(), current)) return false;
        }

        JourneySnapshot lastGood = data.latestRotating(playerId);
        if (isSuspiciousDrop(lastGood, current)) {
            skippedSuspiciousSnapshots++;
            return false;
        }

        long id = nextId();
        data.add(playerId, new JourneySnapshot(id, "auto-" + id, worldTick, SnapshotKind.AUTO, current));
        return true;
    }

    public JourneySnapshot createSafety(UUID playerId, String name, long worldTick, ResearchStateSnapshot current) {
        if (playerId == null || current == null || current.isEmpty()) return null;
        long id = nextId();
        JourneySnapshot snapshot = new JourneySnapshot(
            id,
            name == null || name.isEmpty() ? "safety-" + id : name,
            worldTick,
            SnapshotKind.SAFETY,
            current);
        data.add(playerId, snapshot);
        return snapshot;
    }

    public JourneySnapshot createManual(UUID playerId, String name, long worldTick, ResearchStateSnapshot current) {
        if (playerId == null || current == null) return null;
        long id = nextId();
        JourneySnapshot snapshot = new JourneySnapshot(
            id,
            name == null || name.isEmpty() ? "manual-" + id : name,
            worldTick,
            SnapshotKind.MANUAL,
            current);
        data.add(playerId, snapshot);
        return snapshot;
    }

    public long skippedSuspiciousSnapshots() {
        return skippedSuspiciousSnapshots;
    }

    private static boolean isSuspiciousDrop(JourneySnapshot previous, ResearchStateSnapshot current) {
        if (previous == null || current == null || previous.state().size() < 100) return false;
        return ((long) current.size()) * 4L < previous.state().size();
    }

    private static boolean sameState(ResearchStateSnapshot left, ResearchStateSnapshot right) {
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

    private static long nextId() {
        return NEXT_ID.incrementAndGet();
    }
}
