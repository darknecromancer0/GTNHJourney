package dev.gtnhjourney.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.persistence.JourneyFavouriteData;
import dev.gtnhjourney.recovery.JourneyActionKind;
import dev.gtnhjourney.research.ResearchFingerprint;

/** Moves favourite requests off Netty onto the authoritative server tick. */
public final class FavouriteRequestQueue {

    private static final Queue<Request> REQUESTS = new ConcurrentLinkedQueue<Request>();

    static void enqueue(EntityPlayerMP player, ResearchFingerprint fingerprint, boolean favourite) {
        if (player != null && fingerprint != null) REQUESTS.add(new Request(player, fingerprint, favourite));
    }

    public static void clear() { REQUESTS.clear(); }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Request request;
        int processed = 0;
        while (processed++ < 128 && (request = REQUESTS.poll()) != null) apply(request);
    }

    private static void apply(Request request) {
        EntityPlayerMP player = request.player;
        if (player == null || player.isDead || player.playerNetServerHandler == null || GTNHJourney.RESEARCH == null) return;
        if (GTNHJourney.RESEARCH.resolve(player, request.fingerprint) == null) return;
        World root = DimensionManager.getWorld(0);
        JourneyFavouriteData data = JourneyFavouriteData.get(root == null ? player.worldObj : root);
        boolean before = data.contains(player.getUniqueID(), request.fingerprint);
        boolean changed = data.set(player.getUniqueID(), request.fingerprint, request.favourite);
        boolean after = data.contains(player.getUniqueID(), request.fingerprint);
        if (changed && before != after && GTNHJourney.ACTIONS != null) {
            NBTTagCompound beforeTag = favouriteState(request.fingerprint, before);
            NBTTagCompound afterTag = favouriteState(request.fingerprint, after);
            GTNHJourney.MUTATIONS.notePassiveMutation(player);
            GTNHJourney.ACTIONS.record(
                player,
                JourneyActionKind.FAVOURITE,
                request.favourite ? "Favourite add" : "Favourite remove",
                beforeTag,
                afterTag);
        }
        Journey1124Network.sendFavourites(player, data.snapshot(player.getUniqueID()));
    }

    static NBTTagCompound favouriteState(ResearchFingerprint fingerprint, boolean value) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByteArray("Fingerprint", fingerprint.toBytes());
        tag.setBoolean("Value", value);
        return tag;
    }

    private static final class Request {
        final EntityPlayerMP player;
        final ResearchFingerprint fingerprint;
        final boolean favourite;
        Request(EntityPlayerMP player, ResearchFingerprint fingerprint, boolean favourite) {
            this.player = player;
            this.fingerprint = fingerprint;
            this.favourite = favourite;
        }
    }
}
