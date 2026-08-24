package dev.gtnhjourney.network;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

public final class JourneyNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("gtnhjourney");

    private JourneyNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(RetrieveRequestMessage.Handler.class, RetrieveRequestMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ResearchSyncBeginMessage.Handler.class, ResearchSyncBeginMessage.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(ResearchSyncChunkMessage.Handler.class, ResearchSyncChunkMessage.class, 2, Side.CLIENT);
        CHANNEL.registerMessage(ResearchSyncEndMessage.Handler.class, ResearchSyncEndMessage.class, 3, Side.CLIENT);
        CHANNEL.registerMessage(ResearchUnlockMessage.Handler.class, ResearchUnlockMessage.class, 4, Side.CLIENT);
        CHANNEL.registerMessage(
            ResearchServerOnlyUnlockMessage.Handler.class,
            ResearchServerOnlyUnlockMessage.class,
            5,
            Side.CLIENT);
    }

    public static void requestRetrieve(ResearchKey key, int amount) {
        CHANNEL.sendToServer(new RetrieveRequestMessage(ResearchFingerprint.of(key), amount));
    }

    public static void sendFullSync(EntityPlayerMP player, List<ItemStack> stacks) {
        ServerResearchSyncQueue.start(player, stacks);
    }

    public static void sendUnlock(EntityPlayerMP player, ItemStack stack) {
        if (!ServerResearchSyncQueue.deferUnlockIfActive(player, stack)) sendUnlockImmediate(player, stack);
    }

    static void sendUnlockImmediate(EntityPlayerMP player, ItemStack stack) {
        ResearchKey key = safeKey(stack);
        if (key == null) return;
        if (ItemStackPayloadSizer.canSync(stack)) CHANNEL.sendTo(new ResearchUnlockMessage(stack), player);
        else CHANNEL.sendTo(new ResearchServerOnlyUnlockMessage(), player);
    }

    static void sendSyncBegin(EntityPlayerMP player, int epoch, int availableTotal, int syncableTotal,
        boolean normalizeGtTransientIdentity, boolean resetGtToolTemplateState, boolean normalizeGtChargeEndpoints,
        boolean normalizeIc2ChargeEndpoints, boolean normalizeTconToolWear, boolean normalizeCofhChargeEndpoints) {
        CHANNEL.sendTo(
            new ResearchSyncBeginMessage(
                epoch,
                availableTotal,
                syncableTotal,
                normalizeGtTransientIdentity,
                resetGtToolTemplateState,
                normalizeGtChargeEndpoints,
                normalizeIc2ChargeEndpoints,
                normalizeTconToolWear,
                normalizeCofhChargeEndpoints),
            player);
    }

    static void sendSyncChunk(EntityPlayerMP player, int epoch, List<ItemStack> chunk) {
        CHANNEL.sendTo(new ResearchSyncChunkMessage(epoch, chunk), player);
    }

    static void sendSyncEnd(EntityPlayerMP player, int epoch) {
        CHANNEL.sendTo(new ResearchSyncEndMessage(epoch), player);
    }

    private static ResearchKey safeKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            return dev.gtnhjourney.minecraft.ItemStackKeyFactory.from(stack);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
