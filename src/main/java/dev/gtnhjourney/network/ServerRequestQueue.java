package dev.gtnhjourney.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.persistence.PlayerResearchService;
import dev.gtnhjourney.recovery.JourneyMutationService;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/** Moves packet requests onto the authoritative server tick without relying on version-specific scheduler methods. */
public final class ServerRequestQueue {

    private static final int MAX_QUEUED_REQUESTS = 4096;
    private static final int MAX_PENDING_PER_PLAYER = 32;
    private static final Queue<Request> REQUESTS = new ConcurrentLinkedQueue<Request>();
    private static final AtomicInteger QUEUED = new AtomicInteger();
    private static final PendingRequestLimiter PER_PLAYER = new PendingRequestLimiter(MAX_PENDING_PER_PLAYER);
    private final PlayerResearchService research;
    private final JourneyMutationService mutations;

    public ServerRequestQueue(PlayerResearchService research) {
        this(research, null);
    }

    public ServerRequestQueue(PlayerResearchService research, JourneyMutationService mutations) {
        if (research == null) throw new IllegalArgumentException("research must not be null");
        this.research = research;
        this.mutations = mutations;
    }

    public static void clearPending() {
        REQUESTS.clear();
        QUEUED.set(0);
        PER_PLAYER.clear();
    }

    static void enqueue(EntityPlayerMP player, ResearchFingerprint fingerprint, int amount) {
        enqueueRequest(Request.retrieve(player, fingerprint, amount));
    }

    static void enqueueDelete(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        enqueueRequest(Request.delete(player, fingerprint));
    }

    private static void enqueueRequest(Request request) {
        if (request == null || request.player == null || request.fingerprint == null) return;
        if (!PER_PLAYER.tryAcquire(request.playerId)) return;
        if (QUEUED.incrementAndGet() > MAX_QUEUED_REQUESTS) {
            QUEUED.decrementAndGet();
            PER_PLAYER.release(request.playerId);
            return;
        }
        REQUESTS.add(request);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Request request;
        int processed = 0;
        while (processed++ < 256 && (request = REQUESTS.poll()) != null) {
            QUEUED.decrementAndGet();
            try {
                handle(request);
            } finally {
                PER_PLAYER.release(request.playerId);
            }
        }
    }

    private void handle(Request request) {
        EntityPlayerMP player = request.player;
        if (player == null || player.isDead || player.playerNetServerHandler == null) return;
        if (request.delete) {
            handleDelete(player, request.fingerprint);
            return;
        }
        ResearchKey key = research.resolve(player, request.fingerprint);
        if (key == null) return;
        ItemStack stack = research.retrieve(player, key, request.amount);
        if (stack == null) return;
        player.inventory.addItemStackToInventory(stack);
        if (stack.stackSize > 0) player.dropPlayerItemWithRandomChoice(stack, false);
        player.inventoryContainer.detectAndSendChanges();

        // N is activity history, not inventory-observation history. Only this successful Journey issuance moves an
        // already researched item to the front; later pickup/reconcile passes therefore cannot reorder N again.
        research.recordRetrieval(player, key);
        JourneyNetwork.sendActivityTouch(player, ResearchFingerprint.of(key));
    }

    private void handleDelete(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        if (mutations == null) return;
        ResearchKey key = research.registry(player)
            .find(fingerprint);
        if (key == null) return;
        if (!mutations.deleteExact(player, key, "D delete")) return;
        JourneyNetwork.sendRemove(player, fingerprint);
    }

    private static final class Request {

        final EntityPlayerMP player;
        final java.util.UUID playerId;
        final ResearchFingerprint fingerprint;
        final int amount;
        final boolean delete;

        private Request(EntityPlayerMP player, ResearchFingerprint fingerprint, int amount, boolean delete) {
            this.player = player;
            this.playerId = player == null ? null : player.getUniqueID();
            this.fingerprint = fingerprint;
            this.amount = amount;
            this.delete = delete;
        }

        static Request retrieve(EntityPlayerMP player, ResearchFingerprint fingerprint, int amount) {
            return new Request(player, fingerprint, amount, false);
        }

        static Request delete(EntityPlayerMP player, ResearchFingerprint fingerprint) {
            return new Request(player, fingerprint, 0, true);
        }
    }
}
