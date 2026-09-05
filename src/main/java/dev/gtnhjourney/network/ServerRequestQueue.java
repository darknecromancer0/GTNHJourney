package dev.gtnhjourney.network;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.acquisition.ManualInventoryResearchService;
import dev.gtnhjourney.acquisition.ResearchObservationService;
import dev.gtnhjourney.command.DebugToolPermissionPolicy;
import dev.gtnhjourney.command.JourneyAdminPermissionPolicy;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.persistence.PlayerResearchService;
import dev.gtnhjourney.recovery.JourneyMutationService;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;
import dev.gtnhjourney.retrieval.MainInventoryFillService;

/** Moves packet requests onto the authoritative server tick without relying on version-specific scheduler methods. */
public final class ServerRequestQueue {

    private static final int MAX_QUEUED_REQUESTS = 4096;
    private static final int MAX_PENDING_PER_PLAYER = 32;
    private static final Queue<Request> REQUESTS = new ConcurrentLinkedQueue<Request>();
    private static final AtomicInteger QUEUED = new AtomicInteger();
    private static final PendingRequestLimiter PER_PLAYER = new PendingRequestLimiter(MAX_PENDING_PER_PLAYER);
    private final PlayerResearchService research;
    private final JourneyMutationService mutations;
    private final ResearchObservationService observations;

    public ServerRequestQueue(PlayerResearchService research) { this(research, null); }
    public ServerRequestQueue(PlayerResearchService research, JourneyMutationService mutations) {
        if (research == null) throw new IllegalArgumentException("research must not be null");
        this.research = research;
        this.mutations = mutations;
        this.observations = new ResearchObservationService(research, mutations);
    }

    public static void clearPending() { REQUESTS.clear(); QUEUED.set(0); PER_PLAYER.clear(); }
    static void enqueue(EntityPlayerMP player, ResearchFingerprint fingerprint, int amount) { enqueueRequest(Request.retrieve(player, fingerprint, amount)); }
    static void enqueueFillInventory(EntityPlayerMP player, ResearchFingerprint fingerprint) { enqueueRequest(Request.fillInventory(player, fingerprint)); }
    static void enqueueCreativeIssue(EntityPlayerMP player, ItemStack stack, int amount, boolean fillInventory) { enqueueRequest(Request.creativeIssue(player, stack, amount, fillInventory)); }
    static void enqueueDelete(EntityPlayerMP player, ResearchFingerprint fingerprint) { enqueueRequest(Request.delete(player, fingerprint)); }
    static void enqueueInventoryScan(EntityPlayerMP player) { enqueueRequest(Request.inventoryScan(player)); }
    static void enqueueDebugTool(EntityPlayerMP player) { enqueueRequest(Request.debugTool(player)); }

    static void cancelPending(EntityPlayerMP player) {
        if (player == null) return;
        UUID playerId = player.getUniqueID();
        if (playerId == null) return;
        for (Request request : REQUESTS) {
            if (request == null || !playerId.equals(request.playerId)) continue;
            if (REQUESTS.remove(request)) { QUEUED.decrementAndGet(); PER_PLAYER.release(request.playerId); }
        }
    }

    private static void enqueueRequest(Request request) {
        if (request == null || request.player == null) return;
        if (request.requiresFingerprint() && request.fingerprint == null) return;
        if (request.kind == RequestKind.CREATIVE_ISSUE && (request.creativeStack == null || request.creativeStack.getItem() == null)) return;
        if (!PER_PLAYER.tryAcquire(request.playerId)) return;
        if (QUEUED.incrementAndGet() > MAX_QUEUED_REQUESTS) {
            QUEUED.decrementAndGet(); PER_PLAYER.release(request.playerId); return;
        }
        REQUESTS.add(request);
    }

    @SubscribeEvent public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (event != null && event.player instanceof EntityPlayerMP) cancelPending((EntityPlayerMP) event.player);
    }

    @SubscribeEvent public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Request request;
        int processed = 0;
        while (processed++ < 256 && (request = REQUESTS.poll()) != null) {
            QUEUED.decrementAndGet();
            try { handle(request); } finally { PER_PLAYER.release(request.playerId); }
        }
    }

    private void handle(Request request) {
        EntityPlayerMP player = request.player;
        if (player == null || player.isDead || player.playerNetServerHandler == null) return;
        switch (request.kind) {
            case DELETE: handleDelete(player, request.fingerprint); return;
            case FILL_INVENTORY: handleFillInventory(player, request.fingerprint); return;
            case CREATIVE_ISSUE: handleCreativeIssue(player, request); return;
            case INVENTORY_SCAN: handleInventoryScan(player); return;
            case DEBUG_TOOL: handleDebugTool(player); return;
            case RETRIEVE:
            default: handleRetrieve(player, request); return;
        }
    }

    private void handleRetrieve(EntityPlayerMP player, Request request) {
        ResearchKey key = research.resolve(player, request.fingerprint);
        if (key == null) return;
        ItemStack stack = research.retrieve(player, key, request.amount);
        if (stack == null) return;
        player.inventory.addItemStackToInventory(stack);
        if (stack.stackSize > 0) player.dropPlayerItemWithRandomChoice(stack, false);
        player.inventoryContainer.detectAndSendChanges();
        research.recordRetrieval(player, key);
        ResearchFingerprint fingerprint = ResearchFingerprint.of(key);
        JourneyNetwork.sendActivityTouch(player, fingerprint);
        JourneyNetwork.sendIssuedTouch(player, fingerprint);
    }

    private void handleFillInventory(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        ResearchKey key = research.resolve(player, fingerprint);
        if (key == null) return;
        ItemStack template = research.retrieve(player, key, 1);
        if (template == null) return;
        int filled = MainInventoryFillService.fillEmptyMainSlots(player, template);
        if (filled <= 0) return;
        player.inventoryContainer.detectAndSendChanges();
        research.recordRetrieval(player, key);
        ResearchFingerprint issued = ResearchFingerprint.of(key);
        JourneyNetwork.sendActivityTouch(player, issued);
        JourneyNetwork.sendIssuedTouch(player, issued);
    }

    private void handleCreativeIssue(EntityPlayerMP player, Request request) {
        if (!JourneyAdminPermissionPolicy.mayMutate(player)) {
            tell(player, "C / Creative issuance requires the integrated-server owner or operator permission.");
            return;
        }
        ItemStack template = request.creativeStack == null ? null : request.creativeStack.copy();
        if (template == null || template.getItem() == null) return;
        template.stackSize = 1;
        if (!ItemStackPayloadSizer.canSync(template)) return;
        try { if (ItemStackKeyFactory.from(template) == null) return; }
        catch (IllegalArgumentException ignored) { return; }
        catch (RuntimeException ignored) { return; }
        catch (LinkageError ignored) { return; }

        if (request.fillInventory) {
            int filled = MainInventoryFillService.fillEmptyMainSlots(player, template);
            if (filled <= 0) return;
            recordCreativeIssued(player, template);
            observeCreativeIssue(player, template);
            player.inventoryContainer.detectAndSendChanges();
            JourneyNetwork.sendCreativeIssueSuccess(player, template);
            return;
        }

        int inventoryLimit = player.inventory == null ? 64 : player.inventory.getInventoryStackLimit();
        int max = Math.max(1, Math.min(Math.max(1, inventoryLimit), template.getMaxStackSize()));
        int amount = Math.max(1, Math.min(max, request.amount));
        ItemStack issued = template.copy();
        issued.stackSize = amount;
        player.inventory.addItemStackToInventory(issued);
        if (issued.stackSize > 0) player.dropPlayerItemWithRandomChoice(issued, false);
        recordCreativeIssued(player, template);
        observeCreativeIssue(player, template);
        player.inventoryContainer.detectAndSendChanges();
        JourneyNetwork.sendCreativeIssueSuccess(player, template);
    }

    private void recordCreativeIssued(EntityPlayerMP player, ItemStack template) {
        try {
            ResearchKey key = ItemStackKeyFactory.from(template);
            if (key == null) return;
            research.recordIssued(player, key);
            JourneyNetwork.sendIssuedTouch(player, ResearchFingerprint.of(key));
        } catch (IllegalArgumentException ignored) {
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}
    }

    private void observeCreativeIssue(EntityPlayerMP player, ItemStack template) { observations.observe(player, template); }

    private void handleDelete(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        if (mutations == null) return;
        ResearchKey key = research.registry(player).find(fingerprint);
        if (key == null) return;
        if (!mutations.deleteExact(player, key, "D delete")) return;
        JourneyNetwork.sendRemove(player, fingerprint);
    }

    private void handleInventoryScan(EntityPlayerMP player) {
        if (mutations == null) return;
        ManualInventoryResearchService.Result result = ManualInventoryResearchService.scan(player, research, mutations);
        tell(player, result.summary());
    }

    private void handleDebugTool(EntityPlayerMP player) {
        if (!DebugToolPermissionPolicy.mayUse(player)) {
            tell(player, "Debug Researcher Tool requires the integrated-server owner or operator permission."); return;
        }
        if (GTNHJourney.DEBUG_RESEARCHER_TOOL == null) { tell(player, "Debug Researcher Tool is not registered."); return; }
        ItemStack tool = new ItemStack(GTNHJourney.DEBUG_RESEARCHER_TOOL, 1, 0);
        player.inventory.addItemStackToInventory(tool);
        if (tool.stackSize > 0) player.dropPlayerItemWithRandomChoice(tool, false);
        player.inventoryContainer.detectAndSendChanges();
        tell(player, "Debug Researcher Tool granted. Shift+right-click cycles BLOCK / CONTENTS / AREA_16.");
    }

    private static void tell(EntityPlayerMP player, String text) { if (player != null) player.addChatMessage(new ChatComponentText("[Journey] " + text)); }

    private enum RequestKind { RETRIEVE, FILL_INVENTORY, CREATIVE_ISSUE, DELETE, INVENTORY_SCAN, DEBUG_TOOL }

    private static final class Request {
        final EntityPlayerMP player;
        final UUID playerId;
        final ResearchFingerprint fingerprint;
        final ItemStack creativeStack;
        final int amount;
        final boolean fillInventory;
        final RequestKind kind;
        private Request(EntityPlayerMP player, ResearchFingerprint fingerprint, ItemStack creativeStack, int amount,
            boolean fillInventory, RequestKind kind) {
            this.player = player;
            this.playerId = player == null ? null : player.getUniqueID();
            this.fingerprint = fingerprint;
            this.creativeStack = creativeStack == null ? null : creativeStack.copy();
            if (this.creativeStack != null) this.creativeStack.stackSize = 1;
            this.amount = amount;
            this.fillInventory = fillInventory;
            this.kind = kind == null ? RequestKind.RETRIEVE : kind;
        }
        boolean requiresFingerprint() { return kind == RequestKind.RETRIEVE || kind == RequestKind.FILL_INVENTORY || kind == RequestKind.DELETE; }
        static Request retrieve(EntityPlayerMP player, ResearchFingerprint fingerprint, int amount) { return new Request(player, fingerprint, null, amount, false, RequestKind.RETRIEVE); }
        static Request fillInventory(EntityPlayerMP player, ResearchFingerprint fingerprint) { return new Request(player, fingerprint, null, 0, true, RequestKind.FILL_INVENTORY); }
        static Request creativeIssue(EntityPlayerMP player, ItemStack stack, int amount, boolean fillInventory) { return new Request(player, null, stack, amount, fillInventory, RequestKind.CREATIVE_ISSUE); }
        static Request delete(EntityPlayerMP player, ResearchFingerprint fingerprint) { return new Request(player, fingerprint, null, 0, false, RequestKind.DELETE); }
        static Request inventoryScan(EntityPlayerMP player) { return new Request(player, null, null, 0, false, RequestKind.INVENTORY_SCAN); }
        static Request debugTool(EntityPlayerMP player) { return new Request(player, null, null, 0, false, RequestKind.DEBUG_TOOL); }
    }
}
