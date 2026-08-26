package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Payload-free request; permission and item grant are validated on the authoritative server thread. */
public final class DebugToolRequestMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static final class Handler implements IMessageHandler<DebugToolRequestMessage, IMessage> {

        @Override
        public IMessage onMessage(DebugToolRequestMessage message, MessageContext ctx) {
            ServerRequestQueue.enqueueDebugTool(ctx.getServerHandler().playerEntity);
            return null;
        }
    }
}
