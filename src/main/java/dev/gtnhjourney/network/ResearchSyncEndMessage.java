package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientActivityMirror;
import dev.gtnhjourney.client.ClientIssuedMirror;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.ClientStackMirror;
import io.netty.buffer.ByteBuf;

public final class ResearchSyncEndMessage implements IMessage {
    private int epoch;
    public ResearchSyncEndMessage() {}
    public ResearchSyncEndMessage(int epoch) { this.epoch = epoch; }
    public void fromBytes(ByteBuf buf) { epoch = buf.readInt(); }
    public void toBytes(ByteBuf buf) { buf.writeInt(epoch); }

    public static final class Handler implements IMessageHandler<ResearchSyncEndMessage, IMessage> {
        public IMessage onMessage(ResearchSyncEndMessage message, MessageContext ctx) {
            final int epoch = message.epoch;
            ClientNetworkQueue.enqueue(new Runnable() {
                @Override public void run() {
                    // Research, legacy activity and issuance chronology are one visible snapshot. Never publish a half epoch.
                    if (!ClientActivityMirror.isComplete(epoch) || !ClientIssuedMirror.isComplete(epoch)) {
                        ClientStackMirror.abort(epoch);
                        ClientActivityMirror.abort(epoch);
                        ClientIssuedMirror.abort(epoch);
                        return;
                    }
                    if (ClientStackMirror.finish(epoch)) {
                        ClientActivityMirror.finish(epoch, ClientStackMirror.snapshotKeysInResearchOrder());
                        ClientIssuedMirror.finish(epoch);
                    } else {
                        ClientActivityMirror.abort(epoch);
                        ClientIssuedMirror.abort(epoch);
                    }
                }
            });
            return null;
        }
    }
}
