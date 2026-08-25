package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

/** Incremental server acknowledgement for one exact researched-state removal. */
public final class ResearchRemoveMessage implements IMessage {

    private ResearchFingerprint fingerprint;

    public ResearchRemoveMessage() {}

    public ResearchRemoveMessage(ResearchFingerprint fingerprint) {
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

    public static final class Handler implements IMessageHandler<ResearchRemoveMessage, IMessage> {

        @Override
        public IMessage onMessage(ResearchRemoveMessage message, MessageContext ctx) {
            final ResearchFingerprint fingerprint = message == null ? null : message.fingerprint;
            ClientNetworkQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    ClientStackMirror.remove(fingerprint);
                }
            });
            return null;
        }
    }
}
