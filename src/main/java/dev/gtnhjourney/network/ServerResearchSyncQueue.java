package dev.gtnhjourney.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/** Streams large login/rescan research snapshots over multiple server ticks instead of bursting packets at once. */
public final class ServerResearchSyncQueue {

    private static final int MAX_CHUNKS_PER_TICK = 6;
    private static final int ACTIVITY_ENTRIES_PER_CHUNK = 512;
    private static final AtomicInteger EPOCH = new AtomicInteger();
    private static final Map<UUID, Session> ACTIVE = new LinkedHashMap<UUID, Session>();

    public static void start(EntityPlayerMP player, List<ItemStack> stacks) {
        start(player, stacks, java.util.Collections.<ResearchKey>emptyList());
    }

    public static void start(EntityPlayerMP player, List<ItemStack> stacks, List<ResearchKey> activityOldestFirst) {
        if (player == null) return;
        Session session = Session.create(player, EPOCH.incrementAndGet(), stacks, activityOldestFirst);
        synchronized (ACTIVE) {
            ACTIVE.put(player.getUniqueID(), session);
        }
        JourneyNetwork.sendSyncBegin(
            player,
            session.epoch,
            session.availableTotal,
            session.syncableTotal,
            session.activityTotal,
            JourneyConfig.normalizeGtTransientIdentity(),
            JourneyConfig.resetGtToolTemplateState(),
            JourneyConfig.normalizeGtChargeEndpoints(),
            JourneyConfig.normalizeIc2ChargeEndpoints(),
            JourneyConfig.normalizeTconToolWear(),
            JourneyConfig.normalizeCofhChargeEndpoints());
        if (session.stackChunks.isEmpty() && session.activityChunks.isEmpty()) finishIfCurrent(session);
    }

    /** Keeps incremental mutations in authoritative event order while an older full snapshot is still streaming. */
    public static boolean deferUnlockIfActive(EntityPlayerMP player, ItemStack stack) {
        if (player == null || stack == null || stack.getItem() == null) return false;
        synchronized (ACTIVE) {
            Session session = ACTIVE.get(player.getUniqueID());
            if (session == null) return false;
            session.deferredEvents.add(DeferredEvent.unlock(stack));
            return true;
        }
    }

    /** Keeps exact removals ordered relative to passive unlocks that occur during the same full-sync window. */
    public static boolean deferRemoveIfActive(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        return deferFingerprintEvent(player, fingerprint, DeferredKind.REMOVE);
    }

    /** A J/N retrieval during login sync must be replayed after the older activity snapshot finishes. */
    public static boolean deferActivityTouchIfActive(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        return deferFingerprintEvent(player, fingerprint, DeferredKind.ACTIVITY_TOUCH);
    }

    private static boolean deferFingerprintEvent(
        EntityPlayerMP player,
        ResearchFingerprint fingerprint,
        DeferredKind kind) {
        if (player == null || fingerprint == null) return false;
        synchronized (ACTIVE) {
            Session session = ACTIVE.get(player.getUniqueID());
            if (session == null) return false;
            session.deferredEvents.add(new DeferredEvent(kind, null, fingerprint));
            return true;
        }
    }

    public static void clear() {
        synchronized (ACTIVE) {
            ACTIVE.clear();
        }
    }

    public static void cancel(EntityPlayerMP player) {
        if (player == null) return;
        synchronized (ACTIVE) {
            ACTIVE.remove(player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int budget = MAX_CHUNKS_PER_TICK;
        List<Session> sessions;
        synchronized (ACTIVE) {
            sessions = new ArrayList<Session>(ACTIVE.values());
        }

        for (Session session : sessions) {
            if (budget <= 0) break;
            if (!isCurrent(session)) continue;
            if (!isPlayerUsable(session.player)) {
                removeIfCurrent(session);
                continue;
            }
            while (budget > 0 && session.stackCursor < session.stackChunks.size()) {
                JourneyNetwork.sendSyncChunk(
                    session.player,
                    session.epoch,
                    session.stackChunks.get(session.stackCursor++));
                budget--;
            }
            while (budget > 0 && session.stackCursor >= session.stackChunks.size()
                && session.activityCursor < session.activityChunks.size()) {
                JourneyNetwork.sendActivitySyncChunk(
                    session.player,
                    session.epoch,
                    session.activityChunks.get(session.activityCursor++));
                budget--;
            }
            if (session.stackCursor >= session.stackChunks.size()
                && session.activityCursor >= session.activityChunks.size()) finishIfCurrent(session);
        }
    }

    private static void finishIfCurrent(Session session) {
        List<DeferredEvent> deferred;
        synchronized (ACTIVE) {
            if (ACTIVE.get(session.playerId) != session) return;
            ACTIVE.remove(session.playerId);
            deferred = new ArrayList<DeferredEvent>(session.deferredEvents);
        }
        if (!isPlayerUsable(session.player)) return;
        JourneyNetwork.sendSyncEnd(session.player, session.epoch);
        for (DeferredEvent event : deferred) {
            switch (event.kind) {
                case UNLOCK:
                    JourneyNetwork.sendUnlockImmediate(session.player, event.stack);
                    break;
                case REMOVE:
                    JourneyNetwork.sendRemoveImmediate(session.player, event.fingerprint);
                    break;
                case ACTIVITY_TOUCH:
                    JourneyNetwork.sendActivityTouchImmediate(session.player, event.fingerprint);
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean isCurrent(Session session) {
        synchronized (ACTIVE) {
            return ACTIVE.get(session.playerId) == session;
        }
    }

    private static void removeIfCurrent(Session session) {
        synchronized (ACTIVE) {
            if (ACTIVE.get(session.playerId) == session) ACTIVE.remove(session.playerId);
        }
    }

    private static boolean isPlayerUsable(EntityPlayerMP player) {
        return player != null && !player.isDead && player.playerNetServerHandler != null;
    }

    private enum DeferredKind {
        UNLOCK,
        REMOVE,
        ACTIVITY_TOUCH
    }

    private static final class DeferredEvent {

        final DeferredKind kind;
        final ItemStack stack;
        final ResearchFingerprint fingerprint;

        private DeferredEvent(DeferredKind kind, ItemStack stack, ResearchFingerprint fingerprint) {
            this.kind = kind;
            this.stack = stack;
            this.fingerprint = fingerprint;
        }

        static DeferredEvent unlock(ItemStack stack) {
            return new DeferredEvent(DeferredKind.UNLOCK, stack.copy(), null);
        }
    }

    private static final class Session {

        final EntityPlayerMP player;
        final UUID playerId;
        final int epoch;
        final int availableTotal;
        final int syncableTotal;
        final int activityTotal;
        final List<List<ItemStack>> stackChunks;
        final List<List<ResearchFingerprint>> activityChunks;
        final List<DeferredEvent> deferredEvents = new ArrayList<DeferredEvent>();
        int stackCursor;
        int activityCursor;

        private Session(
            EntityPlayerMP player,
            int epoch,
            int availableTotal,
            int syncableTotal,
            int activityTotal,
            List<List<ItemStack>> stackChunks,
            List<List<ResearchFingerprint>> activityChunks) {
            this.player = player;
            this.playerId = player.getUniqueID();
            this.epoch = epoch;
            this.availableTotal = availableTotal;
            this.syncableTotal = syncableTotal;
            this.activityTotal = activityTotal;
            this.stackChunks = stackChunks;
            this.activityChunks = activityChunks;
        }

        static Session create(
            EntityPlayerMP player,
            int epoch,
            List<ItemStack> stacks,
            List<ResearchKey> activityOldestFirst) {
            List<ItemStack> source = stacks == null ? java.util.Collections.<ItemStack>emptyList() : stacks;
            List<Integer> payloadSizes = new ArrayList<Integer>(source.size());
            for (ItemStack stack : source) {
                payloadSizes.add(Integer.valueOf(ItemStackPayloadSizer.serializedBytes(stack)));
            }

            PayloadChunkPlanner.Plan plan = PayloadChunkPlanner.plan(
                payloadSizes,
                ResearchSyncBudget.MAX_ENTRIES_PER_CHUNK,
                ResearchSyncBudget.TARGET_CHUNK_BYTES,
                ResearchSyncBudget.MAX_SINGLE_ENTRY_BYTES);
            List<List<ItemStack>> stackChunks = new ArrayList<List<ItemStack>>(
                plan.getChunks()
                    .size());
            for (List<Integer> indexChunk : plan.getChunks()) {
                List<ItemStack> chunk = new ArrayList<ItemStack>(indexChunk.size());
                for (Integer index : indexChunk) {
                    ItemStack copy = source.get(index.intValue())
                        .copy();
                    copy.stackSize = 1;
                    chunk.add(copy);
                }
                stackChunks.add(chunk);
            }

            List<List<ResearchFingerprint>> activityChunks = new ArrayList<List<ResearchFingerprint>>();
            int activityTotal = 0;
            if (activityOldestFirst != null) {
                List<ResearchFingerprint> current = null;
                for (ResearchKey key : activityOldestFirst) {
                    if (key == null) continue;
                    if (current == null || current.size() >= ACTIVITY_ENTRIES_PER_CHUNK) {
                        current = new ArrayList<ResearchFingerprint>(ACTIVITY_ENTRIES_PER_CHUNK);
                        activityChunks.add(current);
                    }
                    current.add(ResearchFingerprint.of(key));
                    activityTotal++;
                }
            }
            return new Session(
                player,
                epoch,
                plan.getSourceTotal(),
                plan.getSyncableTotal(),
                activityTotal,
                stackChunks,
                activityChunks);
        }
    }
}
