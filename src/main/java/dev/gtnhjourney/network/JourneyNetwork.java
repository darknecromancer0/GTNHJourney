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
        CHANNEL.registerMessage(DeleteRequestMessage.Handler.class, DeleteRequestMessage.class, 6, Side.SERVER);
        CHANNEL.registerMessage(ResearchRemoveMessage.Handler.class, ResearchRemoveMessage.class, 7, Side.CLIENT);
        CHANNEL.registerMessage(
            ResearchUnlockNotificationMessage.Handler.class,
            ResearchUnlockNotificationMessage.class,
            8,
            Side.CLIENT);
        CHANNEL.registerMessage(
            ResearchActivitySyncChunkMessage.Handler.class,
            ResearchActivitySyncChunkMessage.class,
            9,
            Side.CLIENT);
        CHANNEL.registerMessage(
            ResearchActivityTouchMessage.Handler.class,
            ResearchActivityTouchMessage.class,
            10,
            Side.CLIENT);
        CHANNEL.registerMessage(InventoryScanRequestMessage.Handler.class, InventoryScanRequestMessage.class, 11, Side.SERVER);
        CHANNEL.registerMessage(DebugToolRequestMessage.Handler.class, DebugToolRequestMessage.class, 12, Side.SERVER);
        CHANNEL.registerMessage(FillInventoryRequestMessage.Handler.class, FillInventoryRequestMessage.class, 13, Side.SERVER);
    }

    public static void requestRetrieve(ResearchKey key, int amount) {
        CHANNEL.sendToServer(new RetrieveRequestMessage(ResearchFingerprint.of(key), amount));
    }

    public static void requestFillInventory(ResearchKey key) {
        if (key != null) CHANNEL.sendToServer(new FillInventoryRequestMessage(ResearchFingerprint.of(key)));
    }

    public static void requestDelete(ResearchKey key) {
        if (key != null) CHANNEL.sendToServer(new DeleteRequestMessage(ResearchFingerprint.of(key)));
    }

    public static void requestInventoryScan() {
        CHANNEL.sendToServer(new InventoryScanRequestMessage());
    }

    public static void requestDebugTool() {
        CHANNEL.sendToServer(new DebugToolRequestMessage());
    }

    public static void sendFullSync(EntityPlayerMP player, List<ItemStack> stacks) {
        List<ResearchKey> activity = player == null ? java.util.Collections.<ResearchKey>emptyList()
            : dev.gtnhjourney.GTNHJourney.RESEARCH.snapshotActivityOrder(player);
        ServerResearchSyncQueue.start(player, stacks, activity);
    }

    public static void sendFullSync(EntityPlayerMP player, List<ItemStack> stacks, List<ResearchKey> activityOldestFirst) {
        ServerResearchSyncQueue.start(player, stacks, activityOldestFirst);
    }

    public static void sendUnlock(EntityPlayerMP player, ItemStack stack) {
        if (!ServerResearchSyncQueue.deferUnlockIfActive(player, stack)) sendUnlockImmediate(player, stack);
    }

    public static void sendUnlockNotification(EntityPlayerMP player, ItemStack observed) {
        if (player == null || observed == null || observed.getItem() == null) return;
        String displayName = "item";
        try {
            String candidate = observed.getDisplayName();
            if (candidate != null && !candidate.trim().isEmpty()) displayName = candidate;
        } catch (RuntimeException ignored) {
            try {
                String candidate = observed.getItem().getUnlocalizedName(observed);
                if (candidate != null && !candidate.trim().isEmpty()) displayName = candidate;
            } catch (RuntimeException ignoredAgain) {}
        }
        CHANNEL.sendTo(new ResearchUnlockNotificationMessage(displayName), player);
    }

    static void sendUnlockImmediate(EntityPlayerMP player, ItemStack stack) {
        ResearchKey key = safeKey(stack);
        if (key == null) return;
        if (ItemStackPayloadSizer.canSync(stack)) CHANNEL.sendTo(new ResearchUnlockMessage(stack), player);
        else CHANNEL.sendTo(new ResearchServerOnlyUnlockMessage(), player);
    }

    public static void sendActivityTouch(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        if (player == null || fingerprint == null) return;
        if (!ServerResearchSyncQueue.deferActivityTouchIfActive(player, fingerprint)) {
            sendActivityTouchImmediate(player, fingerprint);
        }
    }

    static void sendActivityTouchImmediate(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        if (player != null && fingerprint != null) CHANNEL.sendTo(new ResearchActivityTouchMessage(fingerprint), player);
    }

    public static void sendRemove(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        if (player == null || fingerprint == null) return;
        if (!ServerResearchSyncQueue.deferRemoveIfActive(player, fingerprint)) sendRemoveImmediate(player, fingerprint);
    }

    static void sendRemoveImmediate(EntityPlayerMP player, ResearchFingerprint fingerprint) {
        if (player != null && fingerprint != null) CHANNEL.sendTo(new ResearchRemoveMessage(fingerprint), player);
    }

    static void sendSyncBegin(EntityPlayerMP player, int epoch, int availableTotal, int syncableTotal, int activityTotal,
        boolean normalizeGtTransientIdentity, boolean resetGtToolTemplateState, boolean normalizeGtChargeEndpoints,
        boolean normalizeIc2ChargeEndpoints, boolean normalizeTconToolWear, boolean normalizeCofhChargeEndpoints) {
        CHANNEL.sendTo(
            new ResearchSyncBeginMessage(
                epoch,
                availableTotal,
                syncableTotal,
                activityTotal,
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

    static void sendActivitySyncChunk(EntityPlayerMP player, int epoch, List<ResearchFingerprint> chunk) {
        CHANNEL.sendTo(new ResearchActivitySyncChunkMessage(epoch, chunk), player);
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
