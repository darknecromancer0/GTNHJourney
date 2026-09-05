package dev.gtnhjourney.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import dev.gtnhjourney.client.ClientFavouriteMirror;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

/** Small auxiliary channel for 1.1.24 favourite state. */
public final class Journey1124Network {

    private static final int MAX_FAVOURITES = 4096;
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("gtnhj1124");

    private Journey1124Network() {}

    public static void init() {
        CHANNEL.registerMessage(SetFavourite.Handler.class, SetFavourite.class, 0, Side.SERVER);
        CHANNEL.registerMessage(FavouriteSync.Handler.class, FavouriteSync.class, 1, Side.CLIENT);
    }

    public static void requestSet(ResearchFingerprint fingerprint, boolean favourite) {
        if (fingerprint != null) CHANNEL.sendToServer(new SetFavourite(fingerprint, favourite));
    }

    public static void sendFavourites(EntityPlayerMP player, List<ResearchFingerprint> values) {
        if (player == null || player.playerNetServerHandler == null) return;
        CHANNEL.sendTo(new FavouriteSync(values), player);
    }

    public static final class SetFavourite implements IMessage {
        private ResearchFingerprint fingerprint;
        private boolean favourite;
        public SetFavourite() {}
        SetFavourite(ResearchFingerprint fingerprint, boolean favourite) {
            this.fingerprint = fingerprint;
            this.favourite = favourite;
        }
        @Override public void fromBytes(ByteBuf buf) {
            fingerprint = ResearchFingerprintBuf.read(buf);
            favourite = buf.readBoolean();
        }
        @Override public void toBytes(ByteBuf buf) {
            ResearchFingerprintBuf.write(buf, fingerprint);
            buf.writeBoolean(favourite);
        }

        public static final class Handler implements IMessageHandler<SetFavourite, IMessage> {
            @Override public IMessage onMessage(SetFavourite message, MessageContext ctx) {
                if (message != null && message.fingerprint != null && ctx.getServerHandler() != null) {
                    FavouriteRequestQueue.enqueue(ctx.getServerHandler().playerEntity, message.fingerprint, message.favourite);
                }
                return null;
            }
        }
    }

    public static final class FavouriteSync implements IMessage {
        private final List<ResearchFingerprint> values = new ArrayList<ResearchFingerprint>();
        public FavouriteSync() {}
        FavouriteSync(List<ResearchFingerprint> source) {
            if (source != null) {
                for (ResearchFingerprint value : source) {
                    if (value != null && values.size() < MAX_FAVOURITES) values.add(value);
                }
            }
        }
        @Override public void fromBytes(ByteBuf buf) {
            values.clear();
            int count = Math.max(0, Math.min(MAX_FAVOURITES, buf.readInt()));
            for (int i = 0; i < count; i++) values.add(ResearchFingerprintBuf.read(buf));
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeInt(values.size());
            for (ResearchFingerprint value : values) ResearchFingerprintBuf.write(buf, value);
        }

        public static final class Handler implements IMessageHandler<FavouriteSync, IMessage> {
            @Override public IMessage onMessage(final FavouriteSync message, MessageContext ctx) {
                if (message == null) return null;
                final List<ResearchFingerprint> copy = new ArrayList<ResearchFingerprint>(message.values);
                ClientNetworkQueue.enqueue(new Runnable() {
                    @Override public void run() { ClientFavouriteMirror.replace(copy); }
                });
                return null;
            }
        }
    }
}
