package dev.gtnhjourney.network;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.ClientPresentationActivityMirror;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;
import io.netty.buffer.ByteBuf;

/** Touches C presentation activity only after the authoritative server confirms a real issuance. */
public final class CreativeIssueSuccessMessage implements IMessage {

    private ItemStack stack;

    public CreativeIssueSuccessMessage() {}

    public CreativeIssueSuccessMessage(ItemStack stack) {
        this.stack = stack == null ? null : stack.copy();
        if (this.stack != null) this.stack.stackSize = 1;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
    }

    public static final class Handler implements IMessageHandler<CreativeIssueSuccessMessage, IMessage> {

        @Override
        public IMessage onMessage(CreativeIssueSuccessMessage message, MessageContext ctx) {
            final ItemStack copy = message == null || message.stack == null ? null : message.stack.copy();
            ClientNetworkQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    if (copy == null || copy.getItem() == null) return;
                    try {
                        ResearchKey key = ItemStackKeyFactory.from(copy);
                        if (key != null) ClientPresentationActivityMirror.touch(key);
                    } catch (IllegalArgumentException ignored) {
                    } catch (RuntimeException ignored) {
                    } catch (LinkageError ignored) {}
                }
            });
            return null;
        }
    }
}
