package dev.gtnhjourney.backup;

import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Starts due automatic backups from the authoritative server END tick. */
public final class WorldBackupTicker {

    private final WorldBackupCoordinator coordinator;

    public WorldBackupTicker(WorldBackupCoordinator coordinator) {
        if (coordinator == null) throw new IllegalArgumentException("coordinator");
        this.coordinator = coordinator;
    }

    static boolean shouldRun(TickEvent.Phase phase, boolean due) {
        return phase == TickEvent.Phase.END && due;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!shouldRun(event.phase, coordinator.isAutomaticDue())) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) coordinator.tryBackup(server, false);
    }
}
