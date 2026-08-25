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

/**
 * Streams large login/rescan research snapshots over multiple server ticks instead of bursting every packet at once.
 */
public final class ServerResearchSyncQueue {

    private static final int MAX_CHUNKS_PER_TICK = 6;
    private static final AtomicInteger EPOCH = new AtomicInteger();
    private static final Map<UUID, Session> ACTIVE = new LinkedHashMap<UUID, Session>();

    public static void start(EntityPlayerMP player, List<ItemStack> stacks) {
        if (player == null) return;
        Session session = Session.create(player, EPOCH.incrementAndGet(), stacks);
        synchronized (ACTIVE) {
            ACTIVE.put(player.getUniqueID(), session);
        }
        JourneyNetwork.sendSyncBegin(
            player,
            session.epoch,
            session.availableTotal,
            session.syncableTotal,
            JourneyConfig.normalizeGtTransientIdentity(),
            JourneyConfig.resetGtToolTemplateState(),
            JourneyConfig.normalizeGtChargeEndpoints(),
            JourneyConfig.normalizeIc2ChargeEndpoints(),
            JourneyConfig.normalizeTconToolWear(),
            JourneyConfig.normalizeCofhChargeEndpoints());
        if (session.chunks.isEmpty()) finishIfCurrent(session);
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
        if (player == null || fingerprint == null) return false;
        synchronized (ACTIVE) {
            Session session = ACTIVE.get(player.getUniqueID());
            if (session == null) return false;
            session.deferredEvents.add(DeferredEvent.remove(fingerprint));
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
            while (budget > 0 && session.cursor < session.chunks.size()) {
                JourneyNetwork.sendSyncChunk(session.player, session.epoch, session.chunks.get(session.cursor++));
                budget--;
            }
            if (session.cursor >= session.chunks.size()) finishIfCurrent(session);
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
            if (event.stack != null) JourneyNetwork.sendUnlockImmediate(session.player, event.stack);
            else JourneyNetwork.sendRemoveImmediate(session.player, event.fingerprint);
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

    private static final class DeferredEvent {

        final ItemStack stack;
        final ResearchFingerprint fingerprint;

        private DeferredEvent(ItemStack stack, ResearchFingerprint fingerprint) {
            this.stack = stack;
            this.fingerprint = fingerprint;
        }

        static DeferredEvent unlock(ItemStack stack) {
            return new DeferredEvent(stack.copy(), null);
        }

        static DeferredEvent remove(ResearchFingerprint fingerprint) {
            return new DeferredEvent(null, fingerprint);
        }
    }

    private static final class Session {

        final EntityPlayerMP player;
        final UUID playerId;
        final int epoch;
        final int availableTotal;
        final int syncableTotal;
        final List<List<ItemStack>> chunks;
        final List<DeferredEvent> deferredEvents = new ArrayList<DeferredEvent>();
        int cursor;

        private Session(EntityPlayerMP player, int epoch, int availableTotal, int syncableTotal,
            List<List<ItemStack>> chunks) {
            this.player = player;
            this.playerId = player.getUniqueID();
            this.epoch = epoch;
            this.availableTotal = availableTotal;
            this.syncableTotal = syncableTotal;
            this.chunks = chunks;
        }

        static Session create(EntityPlayerMP player, int epoch, List<ItemStack> stacks) {
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
            List<List<ItemStack>> chunks = new ArrayList<List<ItemStack>>(
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
                chunks.add(chunk);
            }
            return new Session(player, epoch, plan.getSourceTotal(), plan.getSyncableTotal(), chunks);
        }
    }
}
