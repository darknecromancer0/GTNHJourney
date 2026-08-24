package dev.gtnhjourney.acquisition;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

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
    private final Map<UUID, InventoryScanCache> scanCaches = new HashMap<UUID, InventoryScanCache>();

    public InventoryResearchTracker(PlayerResearchService research) {
        this.research = research;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.worldObj.isRemote || player.ticksExisted % JourneyConfig.inventoryScanIntervalTicks() != 0) return;

        int fullInterval = JourneyConfig.inventoryFullRescanIntervalTicks();
        boolean force = fullInterval > 0 && player.ticksExisted % fullInterval == 0;
        scanChanged(player, force, true);
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player instanceof EntityPlayerMP) unlock((EntityPlayerMP) event.player, event.crafting);
    }

    @SubscribeEvent
    public void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.player instanceof EntityPlayerMP) unlock((EntityPlayerMP) event.player, event.smelting);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        synchronized (scanCaches) {
            scanCaches.remove(player.getUniqueID());
        }
        ServerResearchSyncQueue.cancel(player);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        final EntityPlayerMP player = (EntityPlayerMP) event.player;
        // Import pre-existing inventory research first and prime slot signatures, then send one coherent client
        // snapshot.
        scanChanged(player, true, false);
        JourneyNetwork.sendFullSync(player, research.snapshotStacksInUnlockOrder(player));
    }

    /** Clears process-lifetime scan state between integrated/dedicated server sessions. */
    public void clearCaches() {
        synchronized (scanCaches) {
            scanCaches.clear();
        }
    }

    private void scanChanged(final EntityPlayerMP player, boolean force, final boolean sendIncrementalUnlocks) {
        final InventoryScanCache cache = cache(player.getUniqueID());
        cache.beginPass(force);
        try {
            PlayerInventoryScanner.scanSlots(player, new PlayerInventoryScanner.SlotVisitor() {

                @Override
                public void visit(String slotId, ItemStack stack) {
                    if (!cache.shouldInspect(slotId, InventoryStackSignature.of(stack))) return;
                    if (sendIncrementalUnlocks) unlock(player, stack);
                    else research.unlock(player, stack);
                }
            });
        } finally {
            cache.endPass();
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

    private void unlock(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return;
        for (ItemStack unlocked : research.unlockStates(player, stack)) {
            dev.gtnhjourney.diagnostics.ResearchTrace.unlocked(player, unlocked);
            try {
                JourneyNetwork.sendUnlock(player, unlocked);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
