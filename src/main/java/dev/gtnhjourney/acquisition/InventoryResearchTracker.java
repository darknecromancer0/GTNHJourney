package dev.gtnhjourney.acquisition;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.network.ServerResearchSyncQueue;
import dev.gtnhjourney.persistence.PlayerResearchService;

/** Researches only stacks proven to be in, or entering, a real server-side player inventory. */
public final class InventoryResearchTracker {

    private final PlayerResearchService research;
    private final ResearchObservationService observations;
    private final Map<UUID, InventoryScanCache> scanCaches = new HashMap<UUID, InventoryScanCache>();
    private final ReconcileRequestSet reconcileRequests = new ReconcileRequestSet();

    public InventoryResearchTracker(PlayerResearchService research, ResearchObservationService observations) {
        if (research == null) throw new IllegalArgumentException("research must not be null");
        if (observations == null) throw new IllegalArgumentException("observations must not be null");
        this.research = research;
        this.observations = observations;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.worldObj.isRemote) return;

        if (reconcileRequests.consume(player.getUniqueID())) {
            scanChanged(player, true, true);
            return;
        }
        if (player.ticksExisted % JourneyConfig.inventoryScanIntervalTicks() != 0) return;

        int fullInterval = JourneyConfig.inventoryFullRescanIntervalTicks();
        boolean force = fullInterval > 0 && player.ticksExisted % fullInterval == 0;
        scanChanged(player, force, true);
    }

    /** Pickup is only a hint: authoritative research happens from the player's post-merge inventory on the next tick. */
    @SubscribeEvent
    public void onPickup(EntityItemPickupEvent event) {
        if (!(event.entityPlayer instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        if (player.worldObj.isRemote) return;
        reconcileRequests.request(player.getUniqueID());
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player instanceof EntityPlayerMP) observations.observe((EntityPlayerMP) event.player, event.crafting);
    }

    @SubscribeEvent
    public void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        observations.observe(player, event.smelting);
        // Some furnace/container paths move or merge the result before the next cached inventory pass. Revalidate the
        // real player inventory immediately so ordinary pickup and shift-click do not depend on selecting the stack.
        scanChanged(player, true, true);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        synchronized (scanCaches) {
            scanCaches.remove(player.getUniqueID());
        }
        reconcileRequests.discard(player.getUniqueID());
        CreativeIssueResearchSuppressor.clear(player);
        ServerResearchSyncQueue.cancel(player);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        final EntityPlayerMP player = (EntityPlayerMP) event.player;
        // Import pre-existing inventory research first and prime slot signatures, then send one coherent client
        // snapshot. Login import intentionally bypasses the observation path so it cannot emit unlock notifications.
        scanChanged(player, true, false);
        JourneyNetwork.sendFullSync(
            player,
            research.snapshotStacksInUnlockOrder(player),
            research.snapshotActivityOrder(player));
    }

    /** Clears process-lifetime scan state between integrated/dedicated server sessions. */
    public void clearCaches() {
        synchronized (scanCaches) {
            scanCaches.clear();
        }
        reconcileRequests.clear();
        CreativeIssueResearchSuppressor.clearAll();
    }

    private void scanChanged(final EntityPlayerMP player, boolean force, final boolean sendIncrementalUnlocks) {
        final InventoryScanCache cache = cache(player.getUniqueID());
        cache.beginPass(force);
        try {
            PlayerInventoryScanner.scanSlots(player, new PlayerInventoryScanner.SlotVisitor() {

                @Override
                public void visit(String slotId, ItemStack stack) {
                    if (!cache.shouldInspect(slotId, InventoryStackSignature.of(stack))) return;
                    if (CreativeIssueResearchSuppressor.shouldSuppress(player, stack)) return;
                    if (sendIncrementalUnlocks) observations.observe(player, stack);
                    else research.unlock(player, stack);
                }
            });
        } finally {
            cache.endPass();
            CreativeIssueResearchSuppressor.retainPresent(player);
        }
    }

    private InventoryScanCache cache(UUID playerId) {
        synchronized (scanCaches) {
            InventoryScanCache cache = scanCaches.get(playerId);
            if (cache == null) {
                cache = new InventoryScanCache();
                scanCaches.put(playerId, cache);
            }
            return cache;
        }
    }
}
