package dev.gtnhjourney.recovery;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.persistence.JourneySnapshotData;

/** Runs bounded automatic snapshots from the authoritative server tick. */
public final class JourneySnapshotTicker {

    private long skippedSuspiciousSnapshots;

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

        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        forEachAtCadence(worldTick, players, new CadenceAction<EntityPlayerMP>() {

            @Override
            public void apply(EntityPlayerMP player) {
                if (player.isDead || player.playerNetServerHandler == null) return;
                snapshots.maybeAutoSnapshot(player.getUniqueID(), worldTick, true, research.captureState(player.getUniqueID()));
            }
        });
        skippedSuspiciousSnapshots += snapshots.skippedSuspiciousSnapshots();
    }

    public long skippedSuspiciousSnapshots() {
        return skippedSuspiciousSnapshots;
    }

    public void clear() {
        skippedSuspiciousSnapshots = 0L;
    }

    public interface CadenceAction<T> {

        void apply(T value);
    }
}
