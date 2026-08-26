package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Payload-free request. The server scans only the authenticated sender's player-owned inventory. */
public final class InventoryScanRequestMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static final class Handler implements IMessageHandler<InventoryScanRequestMessage, IMessage> {

        @Override
        public IMessage onMessage(InventoryScanRequestMessage message, MessageContext ctx) {
            ServerRequestQueue.enqueueInventoryScan(ctx.getServerHandler().playerEntity);
            return null;
        }
    }
}
