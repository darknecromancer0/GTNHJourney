package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.ClientStackMirror;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;

public final class ResearchUnlockMessage implements IMessage {
    private ItemStack stack;
    public ResearchUnlockMessage() {}
    public ResearchUnlockMessage(ItemStack stack) { this.stack = stack == null ? null : stack.copy(); }
    public void fromBytes(ByteBuf buf) { stack = ByteBufUtils.readItemStack(buf); }
    public void toBytes(ByteBuf buf) { ByteBufUtils.writeItemStack(buf, stack); }
    public static final class Handler implements IMessageHandler<ResearchUnlockMessage, IMessage> {
        public IMessage onMessage(ResearchUnlockMessage message, MessageContext ctx) {
            final ItemStack copy = message.stack == null ? null : message.stack.copy();
            ClientNetworkQueue.enqueue(new Runnable() {
                @Override public void run() { ClientStackMirror.addUnlock(copy); }
            });
            return null;
        }
    }
}
