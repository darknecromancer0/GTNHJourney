package dev.gtnhjourney.recovery;

import net.minecraft.entity.player.EntityPlayerMP;

/** Merges the legacy research journal with the newer reversible action journal by original action time. */
public final class JourneyUndoCoordinator {

    private final JourneyMutationService research;
    private final JourneyReversibleActionService actions;

    public JourneyUndoCoordinator(JourneyMutationService research, JourneyReversibleActionService actions) {
        if (research == null) throw new IllegalArgumentException("research must not be null");
        if (actions == null) throw new IllegalArgumentException("actions must not be null");
        this.research = research;
        this.actions = actions;
    }

    public Result undo(EntityPlayerMP player, int count) {
        if (player == null) return new Result(0, 0, 0);
        int requested = clamp(count);
        int applied = 0;
        int researchApplied = 0;
        int actionApplied = 0;
        while (applied < requested) {
            long researchTime = JourneyResearchHistoryPeek.undoTimestamp(player);
            JourneyActionTransaction action = actions.peekUndo(player);
            long actionTime = action == null ? Long.MIN_VALUE : action.timestamp();
            if (researchTime == Long.MIN_VALUE && actionTime == Long.MIN_VALUE) break;
            int changed;
            if (actionTime >= researchTime) {
                changed = actions.undo(player, null, 1);
                actionApplied += changed;
            } else {
                changed = research.undo(player, 1);
                researchApplied += changed;
            }
            if (changed <= 0) break;
            applied += changed;
        }
        return new Result(applied, researchApplied, actionApplied);
    }

    public Result redo(EntityPlayerMP player, int count) {
        if (player == null) return new Result(0, 0, 0);
        int requested = clamp(count);
        int applied = 0;
        int researchApplied = 0;
        int actionApplied = 0;
        while (applied < requested) {
            long researchTime = JourneyResearchHistoryPeek.redoTimestamp(player);
            JourneyActionTransaction action = actions.peekRedo(player);
            long actionTime = action == null ? Long.MAX_VALUE : action.timestamp();
            if (researchTime == Long.MIN_VALUE && action == null) break;
            int changed;
            // Redo replays original chronology, so choose the oldest available original action first.
            if (action != null && (researchTime == Long.MIN_VALUE || actionTime <= researchTime)) {
                changed = actions.redo(player, null, 1);
                actionApplied += changed;
            } else {
                changed = research.redo(player, 1);
                researchApplied += changed;
            }
            if (changed <= 0) break;
            applied += changed;
        }
        return new Result(applied, researchApplied, actionApplied);
    }

    private static int clamp(int count) { return Math.max(1, Math.min(100, count)); }

    public static final class Result {
        private final int applied;
        private final int researchApplied;
        private final int actionApplied;

        Result(int applied, int researchApplied, int actionApplied) {
            this.applied = applied;
            this.researchApplied = researchApplied;
            this.actionApplied = actionApplied;
        }

        public int applied() { return applied; }
        public int researchApplied() { return researchApplied; }
        public int actionApplied() { return actionApplied; }
    }
}
