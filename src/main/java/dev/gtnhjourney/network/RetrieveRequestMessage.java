package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

/** Fixed-size retrieval request. Full NBT never travels client -> server on a click. */
public final class RetrieveRequestMessage implements IMessage {

    private ResearchFingerprint fingerprint;
    private int amount;

    public RetrieveRequestMessage() {}

    public RetrieveRequestMessage(ResearchFingerprint fingerprint, int amount) {
        this.fingerprint = fingerprint;
        this.amount = amount;
    }

    public void fromBytes(ByteBuf buf) {
        fingerprint = ResearchFingerprintBuf.read(buf);
        amount = buf.readInt();
    }

    public void toBytes(ByteBuf buf) {
        ResearchFingerprintBuf.write(buf, fingerprint);
        buf.writeInt(amount);
    }

    public static final class Handler implements IMessageHandler<RetrieveRequestMessage, IMessage> {

        public IMessage onMessage(RetrieveRequestMessage message, MessageContext ctx) {
            if (message != null && message.fingerprint != null && ctx.getServerHandler() != null) {
                ServerRequestQueue.enqueue(ctx.getServerHandler().playerEntity, message.fingerprint, message.amount);
            }
            return null;
        }
    }
}
