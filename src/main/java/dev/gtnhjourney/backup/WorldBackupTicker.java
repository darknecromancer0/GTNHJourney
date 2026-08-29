package dev.gtnhjourney.backup;

import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Finalizes background backups, reports completion, and starts due automatic backups on the server END tick. */
public final class WorldBackupTicker {

    private final WorldBackupCoordinator coordinator;
    private final WorldBackupNotifier notifier;

    public WorldBackupTicker(WorldBackupCoordinator coordinator) {
        this(coordinator, new WorldBackupNotifier());
    }

    WorldBackupTicker(WorldBackupCoordinator coordinator, WorldBackupNotifier notifier) {
        if (coordinator == null) throw new IllegalArgumentException("coordinator");
        if (notifier == null) throw new IllegalArgumentException("notifier");
        this.coordinator = coordinator;
        this.notifier = notifier;
    }

    static boolean shouldRun(TickEvent.Phase phase, boolean due) {
        return phase == TickEvent.Phase.END && due;
    }

    static boolean shouldPollCompletion(TickEvent.Phase phase) {
        return phase == TickEvent.Phase.END;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!shouldPollCompletion(event.phase)) return;
        MinecraftServer server = MinecraftServer.getServer();
        WorldBackupResult completed = coordinator.pollCompletion();
        if (completed != null) notifier.notifyCompletion(server, completed, coordinator.lastDurationMillis());
        if (!shouldRun(event.phase, coordinator.isAutomaticDue())) return;
        if (server != null) coordinator.tryBackup(server, false);
    }
}
