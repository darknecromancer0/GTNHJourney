package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientIssuedMirror;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

/** Incremental successful-issuance touch, emitted only after the server actually grants the item. */
public final class ResearchIssuedTouchMessage implements IMessage {
    private ResearchFingerprint fingerprint;
    public ResearchIssuedTouchMessage() {}
    public ResearchIssuedTouchMessage(ResearchFingerprint fingerprint) { this.fingerprint = fingerprint; }
    @Override public void fromBytes(ByteBuf buf) { fingerprint = ResearchFingerprintBuf.read(buf); }
    @Override public void toBytes(ByteBuf buf) { ResearchFingerprintBuf.write(buf, fingerprint); }

    public static final class Handler implements IMessageHandler<ResearchIssuedTouchMessage, IMessage> {
        @Override public IMessage onMessage(ResearchIssuedTouchMessage message, MessageContext ctx) {
            final ResearchFingerprint fingerprint = message == null ? null : message.fingerprint;
            ClientNetworkQueue.enqueue(new Runnable() {
                @Override public void run() { if (fingerprint != null) ClientIssuedMirror.touch(fingerprint); }
            });
            return null;
        }
    }
}
