package dev.gtnhjourney.acquisition;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;

/** Tracks only furnaces a real player interacted with and attributes output to the last user. */
public final class FurnaceOwnershipTracker {

    private final ResearchObservationService observations;
    private final Map<FurnaceKey, TrackedFurnace> tracked = new HashMap<FurnaceKey, TrackedFurnace>();

    public FurnaceOwnershipTracker(ResearchObservationService observations) {
        if (observations == null) throw new IllegalArgumentException("observations must not be null");
        this.observations = observations;
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (event == null || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (!(event.entityPlayer instanceof EntityPlayerMP) || event.world == null || event.world.isRemote) return;
        if (isDebugResearcherInteraction(event.entityPlayer.getHeldItem())) return;

        TileEntity tile = event.world.getTileEntity(event.x, event.y, event.z);
        if (!(tile instanceof TileEntityFurnace)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        TileEntityFurnace furnace = (TileEntityFurnace) tile;
        ItemStack output = furnace.getStackInSlot(2);
        boolean occupied = isOccupied(output);
        int signature = occupied ? InventoryStackSignature.of(output) : 0;

        FurnaceKey key = new FurnaceKey(event.world.provider.dimensionId, event.x, event.y, event.z);
        TrackedFurnace previous = tracked.get(key);
        boolean sameOwner = previous != null && player.getUniqueID().equals(previous.ownerId);
        FurnaceOutputGate gate = sameOwner ? previous.gate : new FurnaceOutputGate();
        boolean claimOutput = claimInteractionOutput(gate, sameOwner, signature, occupied);
        tracked.put(key, new TrackedFurnace(player.getUniqueID(), gate));

        if (claimOutput && output != null) {
            JourneyRuntimeCounters.furnaceOutputObservation();
            List<ItemStack> unlocked = observations.observe(player, output.copy());
            if (!unlocked.isEmpty()) JourneyRuntimeCounters.furnaceOutputUnlock();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || tracked.isEmpty()) return;

        Iterator<Map.Entry<FurnaceKey, TrackedFurnace>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FurnaceKey, TrackedFurnace> mapEntry = iterator.next();
            FurnaceKey key = mapEntry.getKey();
            TrackedFurnace state = mapEntry.getValue();
            WorldServer world = DimensionManager.getWorld(key.dimension());
            if (world == null || !world.blockExists(key.x(), key.y(), key.z())) {
                iterator.remove();
                continue;
            }

            TileEntity tile = world.getTileEntity(key.x(), key.y(), key.z());
            if (!(tile instanceof TileEntityFurnace) || tile.isInvalid()) {
                iterator.remove();
                continue;
            }

            TileEntityFurnace furnace = (TileEntityFurnace) tile;
            ItemStack output = furnace.getStackInSlot(2);
            boolean occupied = isOccupied(output);
            int signature = occupied ? InventoryStackSignature.of(output) : 0;
            if (!state.gate.observe(signature, occupied)) continue;
            JourneyRuntimeCounters.furnaceOutputObservation();

            EntityPlayerMP owner = findOnlinePlayer(state.ownerId);
            if (!isValidOwner(owner) || output == null) continue;
            List<ItemStack> unlocked = observations.observe(owner, output.copy());
            if (!unlocked.isEmpty()) JourneyRuntimeCounters.furnaceOutputUnlock();
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        UUID playerId = ((EntityPlayerMP) event.player).getUniqueID();
        Iterator<TrackedFurnace> iterator = tracked.values().iterator();
        while (iterator.hasNext()) {
            if (playerId.equals(iterator.next().ownerId)) iterator.remove();
        }
    }

    public void clear() {
        tracked.clear();
    }

    /** First ownership claims an existing output; repeat interaction by the same owner requires a real transition. */
    static boolean claimInteractionOutput(FurnaceOutputGate gate, boolean sameOwner, int signature, boolean occupied) {
        if (gate == null) return false;
        return sameOwner ? gate.observe(signature, occupied) : gate.claim(signature, occupied);
    }

    private static boolean isDebugResearcherInteraction(ItemStack held) {
        return held != null && held.getItem() instanceof ItemDebugResearcherTool;
    }

    private static boolean isOccupied(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.stackSize > 0;
    }

    private static boolean isValidOwner(EntityPlayerMP player) {
        return player != null && !player.isDead && player.playerNetServerHandler != null;
    }

    private static EntityPlayerMP findOnlinePlayer(UUID playerId) {
        if (playerId == null) return null;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) return null;
        for (Object entry : server.getConfigurationManager().playerEntityList) {
            if (!(entry instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) entry;
            if (playerId.equals(player.getUniqueID())) return player;
        }
        return null;
    }

    private static final class TrackedFurnace {

        private final UUID ownerId;
        private final FurnaceOutputGate gate;

        private TrackedFurnace(UUID ownerId, FurnaceOutputGate gate) {
            this.ownerId = ownerId;
            this.gate = gate;
        }
    }
}
