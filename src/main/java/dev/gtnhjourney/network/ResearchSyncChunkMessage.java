package dev.gtnhjourney.network;

import java.util.ArrayList;
import java.util.List;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.ClientStackMirror;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;

public final class ResearchSyncChunkMessage implements IMessage {
    private int epoch;
    private final List<ItemStack> stacks = new ArrayList<ItemStack>();
    public ResearchSyncChunkMessage() {}
    public ResearchSyncChunkMessage(int epoch, List<ItemStack> stacks) { this.epoch = epoch; if (stacks != null) this.stacks.addAll(stacks); }
    public void fromBytes(ByteBuf buf) {
        epoch = buf.readInt();
        int count = Math.max(0, Math.min(256, buf.readUnsignedShort()));
        stacks.clear();
        for (int i = 0; i < count; i++) stacks.add(ByteBufUtils.readItemStack(buf));
    }
    public void toBytes(ByteBuf buf) {
        buf.writeInt(epoch);
        int count = Math.min(256, stacks.size());
        buf.writeShort(count);
        for (int i = 0; i < count; i++) ByteBufUtils.writeItemStack(buf, stacks.get(i));
    }
    public static final class Handler implements IMessageHandler<ResearchSyncChunkMessage, IMessage> {
        public IMessage onMessage(ResearchSyncChunkMessage message, MessageContext ctx) {
            final int epoch = message.epoch;
            final List<ItemStack> copy = new ArrayList<ItemStack>(message.stacks.size());
            for (ItemStack stack : message.stacks) copy.add(stack == null ? null : stack.copy());
            ClientNetworkQueue.enqueue(new Runnable() {
                @Override public void run() { ClientStackMirror.addChunk(epoch, copy); }
            });
            return null;
        }
    }
}
