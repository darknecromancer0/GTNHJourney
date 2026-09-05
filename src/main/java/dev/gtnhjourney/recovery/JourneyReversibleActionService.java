package dev.gtnhjourney.recovery;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import dev.gtnhjourney.persistence.JourneyActionHistoryData;

/** Records and replays persistent non-research Journey actions. */
public final class JourneyReversibleActionService {

    private static final AtomicLong NEXT_ID = new AtomicLong(System.currentTimeMillis() * 1000L);
    private final ActionApplier applier;

    public JourneyReversibleActionService(ActionApplier applier) {
        if (applier == null) throw new IllegalArgumentException("applier must not be null");
        this.applier = applier;
    }

    public boolean record(
        EntityPlayerMP player,
        JourneyActionKind kind,
        String description,
        NBTTagCompound before,
        NBTTagCompound after) {
        if (player == null || kind == null || same(before, after)) return false;
        JourneyActionTransaction transaction = new JourneyActionTransaction(
            NEXT_ID.incrementAndGet(),
            System.currentTimeMillis(),
            kind,
            description,
            before,
            after);
        data(player).record(player.getUniqueID(), transaction);
        return true;
    }

    public int undo(EntityPlayerMP player, JourneyActionKind kind, int count) {
        return apply(player, kind, count, false);
    }

    public int redo(EntityPlayerMP player, JourneyActionKind kind, int count) {
        return apply(player, kind, count, true);
    }

    public JourneyActionTransaction peekUndo(EntityPlayerMP player) {
        return player == null ? null : data(player).peekUndo(player.getUniqueID());
    }

    public JourneyActionTransaction peekRedo(EntityPlayerMP player) {
        return player == null ? null : data(player).peekRedo(player.getUniqueID());
    }

    public int undoDepth(EntityPlayerMP player) {
        return player == null ? 0 : data(player).undoDepth(player.getUniqueID());
    }

    public int redoDepth(EntityPlayerMP player) {
        return player == null ? 0 : data(player).redoDepth(player.getUniqueID());
    }

    public void clearRedo(EntityPlayerMP player) {
        if (player != null) data(player).clearRedo(player.getUniqueID());
    }

    private int apply(EntityPlayerMP player, JourneyActionKind kind, int count, boolean forward) {
        if (player == null) return 0;
        int requested = Math.max(1, Math.min(100, count));
        int applied = 0;
        JourneyActionHistoryData history = data(player);
        UUID playerId = player.getUniqueID();
        while (applied < requested) {
            JourneyActionTransaction transaction = forward
                ? history.popRedo(playerId, kind)
                : history.popUndo(playerId, kind);
            if (transaction == null) break;
            boolean ok = false;
            try {
                ok = applier.apply(player, transaction, forward);
            } catch (RuntimeException ignored) {
                ok = false;
            } catch (LinkageError ignored) {
                ok = false;
            }
            if (!ok) {
                if (forward) history.pushRedo(playerId, transaction);
                else history.pushUndo(playerId, transaction);
                break;
            }
            if (forward) history.pushUndo(playerId, transaction);
            else history.pushRedo(playerId, transaction);
            applied++;
        }
        return applied;
    }

    private static JourneyActionHistoryData data(EntityPlayerMP player) {
        World root = DimensionManager.getWorld(0);
        return JourneyActionHistoryData.get(root == null ? player.worldObj : root);
    }

    private static boolean same(NBTTagCompound left, NBTTagCompound right) {
        String a = left == null ? "" : left.toString();
        String b = right == null ? "" : right.toString();
        return a.equals(b);
    }

    public interface ActionApplier {
        boolean apply(EntityPlayerMP player, JourneyActionTransaction transaction, boolean forward);
    }
}
