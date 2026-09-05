package dev.gtnhjourney.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import dev.gtnhjourney.persistence.JourneyFavouriteData;

/** Sends complete favourite membership and added chronology when a player joins. */
public final class FavouriteSyncTracker {

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        if (event == null || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        World root = DimensionManager.getWorld(0);
        JourneyFavouriteData data = JourneyFavouriteData.get(root == null ? player.worldObj : root);
        Journey1124Network.sendFavourites(player, data.snapshotEntries(player.getUniqueID()));
    }
}
