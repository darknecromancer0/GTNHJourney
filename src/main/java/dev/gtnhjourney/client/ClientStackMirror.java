package dev.gtnhjourney.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/** Exact client ItemStack templates used by the NEI frontend. Server sync is the only writer. */
public final class ClientStackMirror {

    private static final Map<ResearchKey, ItemStack> stacks = new LinkedHashMap<ResearchKey, ItemStack>();
    private static final Map<ResearchKey, ItemStack> staging = new LinkedHashMap<ResearchKey, ItemStack>();
    private static int epoch;
    private static boolean syncing;
    private static int serverAvailableTotal;
    private static int expectedSyncedTotal;
    private static int previousServerAvailableTotal;
    private static int previousExpectedSyncedTotal;

    private ClientStackMirror() {}

    public static synchronized void begin(int newEpoch) {
        beginInternal(newEpoch, 0, -1);
    }

    /** Starts a double-buffered sync; the previous visible snapshot remains active until the matching End arrives. */
    public static synchronized void begin(int newEpoch, int availableTotal, int syncableTotal) {
        int available = Math.max(0, availableTotal);
        beginInternal(newEpoch, available, Math.max(0, Math.min(available, syncableTotal)));
    }

    private static void beginInternal(int newEpoch, int availableTotal, int syncableTotal) {
        // Preserve rollback metadata from the last published snapshot. A newer Begin may supersede an in-flight
        // epoch before that older stream reaches End; its advertised totals are staging data, not a visible baseline.
        if (!syncing) {
            previousServerAvailableTotal = serverAvailableTotal;
            previousExpectedSyncedTotal = expectedSyncedTotal;
        }
        epoch = newEpoch;
        syncing = true;
        serverAvailableTotal = Math.max(0, availableTotal);
        expectedSyncedTotal = syncableTotal;
        staging.clear();
    }

    public static synchronized void addChunk(int chunkEpoch, Iterable<ItemStack> chunk) {
        if (!syncing || chunkEpoch != epoch || chunk == null) return;
        for (ItemStack stack : chunk) addInternal(staging, stack);
    }

    /** Discards only the matching staged epoch and restores metadata for the last complete visible snapshot. */
    public static synchronized void abort(int abortEpoch) {
        if (!syncing || abortEpoch != epoch) return;
        staging.clear();
        syncing = false;
        serverAvailableTotal = previousServerAvailableTotal;
        expectedSyncedTotal = previousExpectedSyncedTotal;
    }

    /** Commits the matching complete epoch and reports whether the visible stack snapshot was actually replaced. */
    public static synchronized boolean finish(int finishEpoch) {
        if (!syncing || finishEpoch != epoch) return false;
        if (expectedSyncedTotal >= 0 && staging.size() != expectedSyncedTotal) {
            abort(finishEpoch);
            return false;
        }
        stacks.clear();
        stacks.putAll(staging);
        staging.clear();
        syncing = false;
        previousServerAvailableTotal = serverAvailableTotal;
        previousExpectedSyncedTotal = expectedSyncedTotal;
        ClientResearchMirror.replace(stacks.keySet());
        return true;
    }

    public static synchronized void addUnlock(ItemStack stack) {
        int before = stacks.size();
        ResearchKey key = addInternal(stacks, stack);
        if (key != null && stacks.size() > before) {
            ClientResearchMirror.add(key);
            ClientActivityMirror.recordUnlock(key);
            serverAvailableTotal++;
            expectedSyncedTotal++;
        }
    }

    public static synchronized void addServerOnlyUnlock() {
        serverAvailableTotal++;
    }

    /** Applies one server-authoritative exact removal without rebuilding the global NEI item universe. */
    public static synchronized boolean remove(ResearchFingerprint fingerprint) {
        if (fingerprint == null) return false;
        ResearchKey found = keyForFingerprint(fingerprint);
        if (found == null) {
            // The server also acknowledges deletions for states that were intentionally omitted from client stack
            // sync because their serialized payload was too large. They have no local key/template to remove, but
            // their authoritative membership still contributed to serverAvailableTotal.
            if (serverOnlyCount() <= 0) return false;
            serverAvailableTotal = Math.max(0, serverAvailableTotal - 1);
            previousServerAvailableTotal = serverAvailableTotal;
            previousExpectedSyncedTotal = expectedSyncedTotal;
            return true;
        }
        stacks.remove(found);
        ClientResearchMirror.remove(found);
        ClientActivityMirror.remove(found);
        serverAvailableTotal = Math.max(0, serverAvailableTotal - 1);
        if (expectedSyncedTotal >= 0) expectedSyncedTotal = Math.max(0, expectedSyncedTotal - 1);
        previousServerAvailableTotal = serverAvailableTotal;
        previousExpectedSyncedTotal = expectedSyncedTotal;
        return true;
    }

    public static synchronized List<ItemStack> snapshot() {
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (Map.Entry<ResearchKey, ItemStack> entry : stacks.entrySet()) out.add(
            entry.getValue()
                .copy());
        return Collections.unmodifiableList(out);
    }

    public static synchronized List<ResearchKey> snapshotKeysInResearchOrder() {
        return Collections.unmodifiableList(new ArrayList<ResearchKey>(stacks.keySet()));
    }

    public static synchronized ResearchKey keyForFingerprint(ResearchFingerprint fingerprint) {
        if (fingerprint == null) return null;
        for (ResearchKey key : stacks.keySet()) {
            if (fingerprint.equals(ResearchFingerprint.of(key))) return key;
        }
        return null;
    }

    /** Returns the most recently unlocked templates first. Retained for diagnostics/backward compatibility. */
    public static synchronized List<ItemStack> snapshotNewest(int limit) {
        if (limit <= 0 || stacks.isEmpty()) return Collections.emptyList();
        List<ItemStack> all = new ArrayList<ItemStack>();
        for (Map.Entry<ResearchKey, ItemStack> entry : stacks.entrySet()) all.add(entry.getValue());
        List<ItemStack> out = new ArrayList<ItemStack>(Math.min(limit, all.size()));
        for (int i = all.size() - 1; i >= 0 && out.size() < limit; i--) out.add(
            all.get(i)
                .copy());
        return Collections.unmodifiableList(out);
    }

    public static synchronized ItemStack template(ResearchKey key) {
        ItemStack stack = stacks.get(key);
        return stack == null ? null : stack.copy();
    }

    public static synchronized boolean isSyncing() {
        return syncing;
    }

    public static synchronized int serverAvailableTotal() {
        return serverAvailableTotal;
    }

    public static synchronized int expectedSyncedTotal() {
        return expectedSyncedTotal;
    }

    public static synchronized int serverOnlyCount() {
        return Math.max(0, serverAvailableTotal - expectedSyncedTotal);
    }

    /** Clears all server-provided client state, e.g. when disconnecting from a server. */
    public static synchronized void clear() {
        epoch++;
        syncing = false;
        stacks.clear();
        staging.clear();
        serverAvailableTotal = 0;
        expectedSyncedTotal = 0;
        previousServerAvailableTotal = 0;
        previousExpectedSyncedTotal = 0;
        ClientResearchMirror.clear();
        ClientActivityMirror.clear();
    }

    private static ResearchKey addInternal(Map<ResearchKey, ItemStack> target, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            ResearchKey key = ItemStackKeyFactory.from(stack);
            ItemStack copy = stack.copy();
            copy.stackSize = 1;
            target.put(key, copy);
            return key;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
