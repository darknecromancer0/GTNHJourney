package dev.gtnhjourney.network;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Privileged C-mode issuance request. The server remains authoritative for permission and final amount. */
public final class CreativeIssueRequestMessage implements IMessage {

    private ItemStack stack;
    private int amount;
    private boolean fillInventory;

    public CreativeIssueRequestMessage() {}

    public CreativeIssueRequestMessage(ItemStack stack, int amount, boolean fillInventory) {
        this.stack = stack == null ? null : stack.copy();
        if (this.stack != null) this.stack.stackSize = 1;
        this.amount = amount;
        this.fillInventory = fillInventory;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stack = ByteBufUtils.readItemStack(buf);
        amount = buf.readInt();
        fillInventory = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
        buf.writeInt(amount);
        buf.writeBoolean(fillInventory);
    }

    public static final class Handler implements IMessageHandler<CreativeIssueRequestMessage, IMessage> {

        @Override
        public IMessage onMessage(CreativeIssueRequestMessage message, MessageContext ctx) {
            if (message != null && ctx.getServerHandler() != null) {
                ServerRequestQueue.enqueueCreativeIssue(
                    ctx.getServerHandler().playerEntity,
                    message.stack,
                    message.amount,
                    message.fillInventory);
            }
            return null;
        }
    }
}
