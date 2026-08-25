package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

/** Fixed-size request to delete one exact researched state. Client NBT is never authoritative. */
public final class DeleteRequestMessage implements IMessage {

    private ResearchFingerprint fingerprint;

    public DeleteRequestMessage() {}

    public DeleteRequestMessage(ResearchFingerprint fingerprint) {
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

    public static final class Handler implements IMessageHandler<DeleteRequestMessage, IMessage> {

        @Override
        public IMessage onMessage(DeleteRequestMessage message, MessageContext ctx) {
            if (message != null && message.fingerprint != null && ctx.getServerHandler() != null) {
                ServerRequestQueue.enqueueDelete(ctx.getServerHandler().playerEntity, message.fingerprint);
            }
            return null;
        }
    }
}
