package dev.gtnhjourney.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import dev.gtnhjourney.client.ClientCommandSuggestionState;
import dev.gtnhjourney.client.ClientFavouriteMirror;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

/** Auxiliary channel for 1.1.24+ favourite state and global command completion. */
public final class Journey1124Network {

    private static final int MAX_FAVOURITES = 4096;
    private static final int MAX_COMMAND_SUGGESTIONS = 64;
    private static final int MAX_COMMAND_PREFIX = 256;
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("gtnhj1124");

    private Journey1124Network() {}

    public static void init() {
        CHANNEL.registerMessage(SetFavourite.Handler.class, SetFavourite.class, 0, Side.SERVER);
        CHANNEL.registerMessage(FavouriteSync.Handler.class, FavouriteSync.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(CommandSuggestionRequest.Handler.class, CommandSuggestionRequest.class, 2, Side.SERVER);
        CHANNEL.registerMessage(CommandSuggestionResponse.Handler.class, CommandSuggestionResponse.class, 3, Side.CLIENT);
    }

    public static void requestSet(ResearchFingerprint fingerprint, boolean favourite) {
        if (fingerprint != null) CHANNEL.sendToServer(new SetFavourite(fingerprint, favourite));
    }

    public static void requestCommandSuggestions(long requestId, String prefix) {
        if (prefix == null || !prefix.startsWith("/")) return;
        CHANNEL.sendToServer(new CommandSuggestionRequest(requestId, limit(prefix, MAX_COMMAND_PREFIX)));
    }

    public static void sendFavourites(EntityPlayerMP player, List<ResearchFingerprint> values) {
        if (player == null || player.playerNetServerHandler == null) return;
        CHANNEL.sendTo(new FavouriteSync(values), player);
    }

    static void sendCommandSuggestions(EntityPlayerMP player, long requestId, String prefix, List<String> values) {
        if (player == null || player.playerNetServerHandler == null) return;
        CHANNEL.sendTo(new CommandSuggestionResponse(requestId, prefix, values), player);
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

    public static final class CommandSuggestionRequest implements IMessage {
        private long requestId;
        private String prefix = "";
        public CommandSuggestionRequest() {}
        CommandSuggestionRequest(long requestId, String prefix) {
            this.requestId = requestId;
            this.prefix = limit(prefix, MAX_COMMAND_PREFIX);
        }
        @Override public void fromBytes(ByteBuf buf) {
            requestId = buf.readLong();
            prefix = limit(ByteBufUtils.readUTF8String(buf), MAX_COMMAND_PREFIX);
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeLong(requestId);
            ByteBufUtils.writeUTF8String(buf, prefix == null ? "" : prefix);
        }

        public static final class Handler implements IMessageHandler<CommandSuggestionRequest, IMessage> {
            @Override public IMessage onMessage(CommandSuggestionRequest message, MessageContext ctx) {
                if (message != null && ctx.getServerHandler() != null && message.prefix.startsWith("/")) {
                    CommandSuggestionRequestQueue.enqueue(
                        ctx.getServerHandler().playerEntity,
                        message.requestId,
                        message.prefix);
                }
                return null;
            }
        }
    }

    public static final class CommandSuggestionResponse implements IMessage {
        private long requestId;
        private String prefix = "";
        private final List<String> values = new ArrayList<String>();
        public CommandSuggestionResponse() {}
        CommandSuggestionResponse(long requestId, String prefix, List<String> source) {
            this.requestId = requestId;
            this.prefix = limit(prefix, MAX_COMMAND_PREFIX);
            if (source != null) {
                for (String value : source) {
                    if (value == null || value.isEmpty()) continue;
                    values.add(limit(value, MAX_COMMAND_PREFIX));
                    if (values.size() >= MAX_COMMAND_SUGGESTIONS) break;
                }
            }
        }
        @Override public void fromBytes(ByteBuf buf) {
            requestId = buf.readLong();
            prefix = limit(ByteBufUtils.readUTF8String(buf), MAX_COMMAND_PREFIX);
            values.clear();
            int count = Math.max(0, Math.min(MAX_COMMAND_SUGGESTIONS, buf.readInt()));
            for (int i = 0; i < count; i++) values.add(limit(ByteBufUtils.readUTF8String(buf), MAX_COMMAND_PREFIX));
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeLong(requestId);
            ByteBufUtils.writeUTF8String(buf, prefix == null ? "" : prefix);
            buf.writeInt(values.size());
            for (String value : values) ByteBufUtils.writeUTF8String(buf, value == null ? "" : value);
        }

        public static final class Handler implements IMessageHandler<CommandSuggestionResponse, IMessage> {
            @Override public IMessage onMessage(final CommandSuggestionResponse message, MessageContext ctx) {
                if (message == null) return null;
                final long requestId = message.requestId;
                final String prefix = message.prefix;
                final List<String> copy = new ArrayList<String>(message.values);
                ClientNetworkQueue.enqueue(new Runnable() {
                    @Override public void run() { ClientCommandSuggestionState.apply(requestId, prefix, copy); }
                });
                return null;
            }
        }
    }

    private static String limit(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
