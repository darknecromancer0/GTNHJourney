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
 *
 * <p>The work deadline is checked only between complete global passes. A pass is never cut in the middle because doing
 * so can advance GT energy buffers/producers without advancing downstream consumers by the same number of extra ticks.</p>
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

        List<WorldSnapshot> snapshots = snapshotWorlds(server.worldServers);
        if (snapshots.isEmpty()) return;

        long deadline = System.nanoTime() + EXTRA_WORK_BUDGET_NANOS;
        int extraPasses = multiplier - 1;
        for (int pass = 0; pass < extraPasses; pass++) {
            if (pass > 0 && System.nanoTime() >= deadline) return;
            tickCompletePass(snapshots);
        }
    }

    private static List<WorldSnapshot> snapshotWorlds(WorldServer[] worlds) {
        List<WorldSnapshot> snapshots = new ArrayList<WorldSnapshot>();
        for (WorldServer world : worlds) {
            if (world == null) continue;
            @SuppressWarnings("unchecked")
            List<TileEntity> tiles = new ArrayList<TileEntity>(world.loadedTileEntityList);
            snapshots.add(new WorldSnapshot(world, tiles));
        }
        return snapshots;
    }

    private static void tickCompletePass(List<WorldSnapshot> snapshots) {
        for (WorldSnapshot snapshot : snapshots) {
            WorldServer world = snapshot.world;
            for (TileEntity tile : snapshot.tiles) {
                if (tile == null || tile.isInvalid() || !tile.canUpdate() || tile.getWorldObj() != world) continue;
                tile.updateEntity();
            }
        }
    }

    private static final class WorldSnapshot {
        final WorldServer world;
        final List<TileEntity> tiles;
        WorldSnapshot(WorldServer world, List<TileEntity> tiles) {
            this.world = world;
            this.tiles = tiles;
        }
    }
}
