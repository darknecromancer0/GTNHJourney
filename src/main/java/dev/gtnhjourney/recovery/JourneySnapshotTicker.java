package dev.gtnhjourney.recovery;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.persistence.JourneySnapshotData;

/** Runs bounded automatic snapshots from the authoritative server tick. */
public final class JourneySnapshotTicker {

    private long skippedSuspiciousSnapshots;
    private long externalSnapshotFailures;

    public static boolean isCadenceTick(long worldTick) {
        return worldTick > 0L && worldTick % JourneySnapshotService.AUTO_INTERVAL_TICKS == 0L;
    }

    public static <T> int forEachAtCadence(long worldTick, Iterable<T> values, CadenceAction<T> action) {
        if (!isCadenceTick(worldTick) || values == null || action == null) return 0;
        int applied = 0;
        for (T value : values) {
            if (value == null) continue;
            try {
                action.apply(value);
                applied++;
            } catch (RuntimeException ignored) {
                // Isolate one broken optional-mod/player state from the rest of the cadence pass.
            } catch (LinkageError ignored) {
                // Optional mod linkage failures must not abort the server tick.
            }
        }
        return applied;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        final World root = DimensionManager.getWorld(0);
        if (root == null) return;
        final long worldTick = root.getTotalWorldTime();
        if (!isCadenceTick(worldTick)) return;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) return;
        final JourneySnapshotService snapshots = new JourneySnapshotService(JourneySnapshotData.get(root));
        final JourneyResearchData research = JourneyResearchData.get(root);
        final File worldDirectory = DimensionManager.getCurrentSaveRootDirectory();
        final File instanceRoot = ExternalJourneySnapshotArchive.instanceRootFor(worldDirectory);
        final String worldName = root.getWorldInfo() == null ? (worldDirectory == null ? "world" : worldDirectory.getName())
            : root.getWorldInfo().getWorldName();

        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        forEachAtCadence(worldTick, players, new CadenceAction<EntityPlayerMP>() {

            @Override
            public void apply(EntityPlayerMP player) {
                if (player.isDead || player.playerNetServerHandler == null) return;
                ResearchStateSnapshot current = research.captureState(player.getUniqueID());
                if (!snapshots.maybeAutoSnapshot(player.getUniqueID(), worldTick, true, current)) return;
                try {
                    ExternalJourneySnapshotArchive.write(
                        instanceRoot,
                        worldName,
                        player.getUniqueID(),
                        worldTick,
                        System.currentTimeMillis(),
                        current);
                } catch (IOException failure) {
                    externalSnapshotFailures++;
                    FMLLog.warning("[GTNH Journey] External research snapshot failed safely: %s", safeMessage(failure));
                } catch (RuntimeException failure) {
                    externalSnapshotFailures++;
                    FMLLog.warning("[GTNH Journey] External research snapshot failed safely: %s", safeMessage(failure));
                } catch (LinkageError failure) {
                    externalSnapshotFailures++;
                    FMLLog.warning("[GTNH Journey] External research snapshot failed safely: %s", safeMessage(failure));
                }
            }
        });
        skippedSuspiciousSnapshots += snapshots.skippedSuspiciousSnapshots();
    }

    public long skippedSuspiciousSnapshots() {
        return skippedSuspiciousSnapshots;
    }

    public long externalSnapshotFailures() {
        return externalSnapshotFailures;
    }

    public void clear() {
        skippedSuspiciousSnapshots = 0L;
        externalSnapshotFailures = 0L;
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isEmpty() ? failure.getClass().getSimpleName() : message;
    }

    public interface CadenceAction<T> {

        void apply(T value);
    }
}
