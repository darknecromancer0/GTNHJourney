package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.ClientStackMirror;
import io.netty.buffer.ByteBuf;

/** Lightweight notice for a researched state intentionally withheld from client ItemStack sync due to NBT size. */
public final class ResearchServerOnlyUnlockMessage implements IMessage {
    public ResearchServerOnlyUnlockMessage() {}
    @Override public void fromBytes(ByteBuf buf) {}
    @Override public void toBytes(ByteBuf buf) {}

    public static final class Handler implements IMessageHandler<ResearchServerOnlyUnlockMessage, IMessage> {
        @Override public IMessage onMessage(ResearchServerOnlyUnlockMessage message, MessageContext ctx) {
            ClientNetworkQueue.enqueue(new Runnable() {
                @Override public void run() { ClientStackMirror.addServerOnlyUnlock(); }
            });
            return null;
        }
    }
}
