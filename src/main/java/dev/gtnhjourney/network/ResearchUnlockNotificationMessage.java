package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.UnlockNotificationService;
import io.netty.buffer.ByteBuf;

/** Small client-bound message carrying only a bounded display name. */
public final class ResearchUnlockNotificationMessage implements IMessage {

    private String displayName;

    public ResearchUnlockNotificationMessage() {}

    public ResearchUnlockNotificationMessage(String displayName) {
        this.displayName = sanitize(displayName);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        displayName = sanitize(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, sanitize(displayName));
    }

    static String sanitize(String value) {
        String safe = value == null ? "item" : value.replace('\n', ' ').replace('\r', ' ').trim();
        if (safe.isEmpty()) safe = "item";
        return safe.length() <= 256 ? safe : safe.substring(0, 256);
    }

    public static final class Handler implements IMessageHandler<ResearchUnlockNotificationMessage, IMessage> {

        @Override
        public IMessage onMessage(ResearchUnlockNotificationMessage message, MessageContext ctx) {
            final String safe = sanitize(message == null ? null : message.displayName);
            ClientNetworkQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    UnlockNotificationService.show(safe);
                }
            });
            return null;
        }
    }
}
