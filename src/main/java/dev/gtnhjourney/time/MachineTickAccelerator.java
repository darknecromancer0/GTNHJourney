package dev.gtnhjourney.time;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

/**
 * Best-effort machine-only acceleration. It adds extra updates only for already-loaded tickable TileEntities while
 * MinecraftServer, world time, entities, weather and random block ticks remain at the normal 20 TPS cadence.
 */
public final class MachineTickAccelerator {

    private static final long EXTRA_WORK_BUDGET_NANOS = 40_000_000L;

    private final JourneySpeedController speed;

    public MachineTickAccelerator(JourneySpeedController speed) {
        if (speed == null) throw new IllegalArgumentException("speed must not be null");
        this.speed = speed;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (speed.mode() != JourneySpeedMode.MACHINES) return;
        int multiplier = speed.multiplier();
        if (multiplier <= 1) return;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null) return;

        long deadline = System.nanoTime() + EXTRA_WORK_BUDGET_NANOS;
        int extraPasses = multiplier - 1;
        for (WorldServer world : server.worldServers) {
            if (world == null || System.nanoTime() >= deadline) break;
            tickWorldTileEntities(world, extraPasses, deadline);
        }
    }

    private static void tickWorldTileEntities(WorldServer world, int extraPasses, long deadline) {
        @SuppressWarnings("unchecked")
        List<TileEntity> snapshot = new ArrayList<TileEntity>(world.loadedTileEntityList);
        for (int pass = 0; pass < extraPasses; pass++) {
            if (System.nanoTime() >= deadline) return;
            for (TileEntity tile : snapshot) {
                if (tile == null || tile.isInvalid() || !tile.canUpdate() || tile.getWorldObj() != world) continue;
                tile.updateEntity();
                if (System.nanoTime() >= deadline) return;
            }
        }
    }
}
