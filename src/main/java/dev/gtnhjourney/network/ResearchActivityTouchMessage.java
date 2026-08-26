package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientActivityMirror;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;
import io.netty.buffer.ByteBuf;

/** Moves one already researched state to the top of N after a successful J/N retrieval. */
public final class ResearchActivityTouchMessage implements IMessage {

    private ResearchFingerprint fingerprint;

    public ResearchActivityTouchMessage() {}

    public ResearchActivityTouchMessage(ResearchFingerprint fingerprint) {
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

    public static final class Handler implements IMessageHandler<ResearchActivityTouchMessage, IMessage> {

        @Override
        public IMessage onMessage(ResearchActivityTouchMessage message, MessageContext ctx) {
            final ResearchFingerprint fingerprint = message == null ? null : message.fingerprint;
            ClientNetworkQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    ResearchKey key = ClientStackMirror.keyForFingerprint(fingerprint);
                    if (key != null) ClientActivityMirror.recordRetrieval(key);
                }
            });
            return null;
        }
    }
}
