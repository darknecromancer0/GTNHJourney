package dev.gtnhjourney.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.persistence.PlayerResearchService;
import dev.gtnhjourney.research.ResearchFingerprint;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** Moves packet requests onto the authoritative server tick without relying on version-specific scheduler methods. */
public final class ServerRequestQueue {
    private static final int MAX_QUEUED_REQUESTS = 4096;
    private static final int MAX_PENDING_PER_PLAYER = 32;
    private static final Queue<Request> REQUESTS = new ConcurrentLinkedQueue<Request>();
    private static final AtomicInteger QUEUED = new AtomicInteger();
    private static final PendingRequestLimiter PER_PLAYER = new PendingRequestLimiter(MAX_PENDING_PER_PLAYER);
    private final PlayerResearchService research;
    public ServerRequestQueue(PlayerResearchService research) { this.research = research; }
    public static void clearPending() {
        REQUESTS.clear();
        QUEUED.set(0);
        PER_PLAYER.clear();
    }

    static void enqueue(EntityPlayerMP player, ResearchFingerprint fingerprint, int amount) {
        if (player == null || fingerprint == null) return;
        if (!PER_PLAYER.tryAcquire(player.getUniqueID())) return;
        if (QUEUED.incrementAndGet() > MAX_QUEUED_REQUESTS) {
            QUEUED.decrementAndGet();
            PER_PLAYER.release(player.getUniqueID());
            return;
        }
        REQUESTS.add(new Request(player, fingerprint, amount));
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
        ItemStack stack = research.retrieve(player, request.fingerprint, request.amount);
        if (stack == null) return;
        player.inventory.addItemStackToInventory(stack);
        if (stack.stackSize > 0) player.dropPlayerItemWithRandomChoice(stack, false);
        player.inventoryContainer.detectAndSendChanges();
    }

    private static final class Request {
        final EntityPlayerMP player;
        final java.util.UUID playerId;
        final ResearchFingerprint fingerprint;
        final int amount;
        Request(EntityPlayerMP player, ResearchFingerprint fingerprint, int amount) {
            this.player = player;
            this.playerId = player.getUniqueID();
            this.fingerprint = fingerprint;
            this.amount = amount;
        }
    }
}
