package dev.gtnhjourney.recovery;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.persistence.DeathInventoryRecoveryData;

/** Watches keepInventory deaths and retains a recovery snapshot if the respawned inventory differs. */
public final class DeathInventoryGuard {

    private static final int POST_RESPAWN_DELAY_TICKS = 2;
    private final Map<UUID, Integer> pending = new HashMap<UUID, Integer>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (event == null || !(event.entityLiving instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
        if (!keepInventory(player)) return;
        data(player).capturePre(player.getUniqueID(), System.currentTimeMillis(), DeathInventorySnapshot.capture(player));
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        if (event == null || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        DeathInventoryRecoveryData.Record record = data(player).record(player.getUniqueID());
        if (record == null || record.pre() == null || !keepInventory(player)) return;
        pending.put(player.getUniqueID(), Integer.valueOf(POST_RESPAWN_DELAY_TICKS));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID playerId = player.getUniqueID();
        Integer remaining = pending.get(playerId);
        if (remaining == null) return;
        if (remaining.intValue() > 0) {
            pending.put(playerId, Integer.valueOf(remaining.intValue() - 1));
            return;
        }
        pending.remove(playerId);
        compareAfterRespawn(player);
    }

    public void clear() { pending.clear(); }

    private void compareAfterRespawn(EntityPlayerMP player) {
        DeathInventoryRecoveryData recovery = data(player);
        DeathInventoryRecoveryData.Record record = recovery.record(player.getUniqueID());
        if (record == null || record.pre() == null) return;
        DeathInventorySnapshot post = DeathInventorySnapshot.capture(player);
        boolean changed = !record.pre().sameContents(post);
        int missing = record.pre().missingUnitsComparedWith(post);
        recovery.capturePost(player.getUniqueID(), post, changed);
        if (!changed) return;
        String detail = missing > 0 ? " Missing item units: " + missing + "." : " Slot/content state changed.";
        player.addChatMessage(new ChatComponentText(
            "[Journey] keepInventory mismatch detected after death." + detail
                + " Recovery snapshot saved. Use /journey return death inventory."));
    }

    private static boolean keepInventory(EntityPlayerMP player) {
        try {
            return player != null && player.worldObj != null && player.worldObj.getGameRules()
                .getGameRuleBooleanValue("keepInventory");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static DeathInventoryRecoveryData data(EntityPlayerMP player) {
        World root = DimensionManager.getWorld(0);
        return DeathInventoryRecoveryData.get(root == null ? player.worldObj : root);
    }
}
