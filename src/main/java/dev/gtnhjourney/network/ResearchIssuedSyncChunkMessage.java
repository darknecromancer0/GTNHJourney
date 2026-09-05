package dev.gtnhjourney.network;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientIssuedMirror;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

/** Compact oldest-first successful-issuance fingerprints for one full research-sync epoch. */
public final class ResearchIssuedSyncChunkMessage implements IMessage {
    private static final int MAX_ENTRIES = 512;
    private int epoch;
    private final List<ResearchFingerprint> fingerprints = new ArrayList<ResearchFingerprint>();

    public ResearchIssuedSyncChunkMessage() {}
    public ResearchIssuedSyncChunkMessage(int epoch, List<ResearchFingerprint> fingerprints) {
        this.epoch = epoch;
        if (fingerprints != null) this.fingerprints.addAll(fingerprints);
    }

    @Override public void fromBytes(ByteBuf buf) {
        epoch = buf.readInt();
        int count = Math.max(0, Math.min(MAX_ENTRIES, buf.readUnsignedShort()));
        fingerprints.clear();
        for (int i = 0; i < count; i++) fingerprints.add(ResearchFingerprintBuf.read(buf));
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(epoch);
        int count = Math.min(MAX_ENTRIES, fingerprints.size());
        buf.writeShort(count);
        for (int i = 0; i < count; i++) ResearchFingerprintBuf.write(buf, fingerprints.get(i));
    }

    public static final class Handler implements IMessageHandler<ResearchIssuedSyncChunkMessage, IMessage> {
        @Override public IMessage onMessage(ResearchIssuedSyncChunkMessage message, MessageContext ctx) {
            final int epoch = message.epoch;
            final List<ResearchFingerprint> copy = new ArrayList<ResearchFingerprint>(message.fingerprints);
            ClientNetworkQueue.enqueue(new Runnable() {
                @Override public void run() { ClientIssuedMirror.addChunk(epoch, copy); }
            });
            return null;
        }
    }
}
