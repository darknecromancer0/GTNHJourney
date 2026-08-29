package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

/** Fixed-size request to fill empty main-inventory slots from one server-authoritative researched state. */
public final class FillInventoryRequestMessage implements IMessage {

    private ResearchFingerprint fingerprint;

    public FillInventoryRequestMessage() {}

    public FillInventoryRequestMessage(ResearchFingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        fingerprint = ResearchFingerprintBuf.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ResearchFingerprintBuf.write(buf, fingerprint);
    }

    public static final class Handler implements IMessageHandler<FillInventoryRequestMessage, IMessage> {

        @Override
        public IMessage onMessage(FillInventoryRequestMessage message, MessageContext ctx) {
            if (message != null && message.fingerprint != null && ctx.getServerHandler() != null) {
                ServerRequestQueue.enqueueFillInventory(ctx.getServerHandler().playerEntity, message.fingerprint);
            }
            return null;
        }
    }
}
