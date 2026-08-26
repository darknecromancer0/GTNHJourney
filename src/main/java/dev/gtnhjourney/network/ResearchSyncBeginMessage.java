package dev.gtnhjourney.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.gtnhjourney.client.ClientActivityMirror;
import dev.gtnhjourney.client.ClientNetworkQueue;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.minecraft.ResearchCompatibilityOptions;
import io.netty.buffer.ByteBuf;

/** Starts an authoritative server research sync and establishes the identity rules used to decode its stacks. */
public final class ResearchSyncBeginMessage implements IMessage {

    private int epoch;
    private int availableTotal;
    private int syncableTotal;
    private boolean normalizeGtTransientIdentity;
    private boolean resetGtToolTemplateState;
    private boolean normalizeGtChargeEndpoints;
    private boolean normalizeIc2ChargeEndpoints;
    private boolean normalizeTconToolWear;
    private boolean normalizeCofhChargeEndpoints;

    public ResearchSyncBeginMessage() {}

    public ResearchSyncBeginMessage(int epoch, int availableTotal, int syncableTotal,
        boolean normalizeGtTransientIdentity, boolean resetGtToolTemplateState, boolean normalizeGtChargeEndpoints,
        boolean normalizeIc2ChargeEndpoints, boolean normalizeTconToolWear, boolean normalizeCofhChargeEndpoints) {
        this.epoch = epoch;
        this.availableTotal = Math.max(0, availableTotal);
        this.syncableTotal = Math.max(0, Math.min(this.availableTotal, syncableTotal));
        this.normalizeGtTransientIdentity = normalizeGtTransientIdentity;
        this.resetGtToolTemplateState = resetGtToolTemplateState;
        this.normalizeGtChargeEndpoints = normalizeGtChargeEndpoints;
        this.normalizeIc2ChargeEndpoints = normalizeIc2ChargeEndpoints;
        this.normalizeTconToolWear = normalizeTconToolWear;
        this.normalizeCofhChargeEndpoints = normalizeCofhChargeEndpoints;
    }

    public void fromBytes(ByteBuf buf) {
        epoch = buf.readInt();
        availableTotal = Math.max(0, buf.readInt());
        syncableTotal = Math.max(0, Math.min(availableTotal, buf.readInt()));
        normalizeGtTransientIdentity = buf.readBoolean();
        resetGtToolTemplateState = buf.readBoolean();
        normalizeGtChargeEndpoints = buf.readBoolean();
        normalizeIc2ChargeEndpoints = buf.readBoolean();
        normalizeTconToolWear = buf.readBoolean();
        normalizeCofhChargeEndpoints = buf.readBoolean();
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(epoch);
        buf.writeInt(availableTotal);
        buf.writeInt(syncableTotal);
        buf.writeBoolean(normalizeGtTransientIdentity);
        buf.writeBoolean(resetGtToolTemplateState);
        buf.writeBoolean(normalizeGtChargeEndpoints);
        buf.writeBoolean(normalizeIc2ChargeEndpoints);
        buf.writeBoolean(normalizeTconToolWear);
        buf.writeBoolean(normalizeCofhChargeEndpoints);
    }

    public static final class Handler implements IMessageHandler<ResearchSyncBeginMessage, IMessage> {

        public IMessage onMessage(final ResearchSyncBeginMessage message, MessageContext ctx) {
            final int epoch = message.epoch;
            final int availableTotal = message.availableTotal;
            final int syncableTotal = message.syncableTotal;
            final boolean normalizeGtTransientIdentity = message.normalizeGtTransientIdentity;
            final boolean resetGtToolTemplateState = message.resetGtToolTemplateState;
            final boolean normalizeGtChargeEndpoints = message.normalizeGtChargeEndpoints;
            final boolean normalizeIc2ChargeEndpoints = message.normalizeIc2ChargeEndpoints;
            final boolean normalizeTconToolWear = message.normalizeTconToolWear;
            final boolean normalizeCofhChargeEndpoints = message.normalizeCofhChargeEndpoints;
            ClientNetworkQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    ResearchCompatibilityOptions.configure(
                        normalizeGtTransientIdentity,
                        resetGtToolTemplateState,
                        normalizeGtChargeEndpoints,
                        normalizeIc2ChargeEndpoints,
                        normalizeTconToolWear,
                        normalizeCofhChargeEndpoints);
                    ClientStackMirror.begin(epoch, availableTotal, syncableTotal);
                    ClientActivityMirror.begin(epoch, availableTotal);
                }
            });
            return null;
        }
    }
}
