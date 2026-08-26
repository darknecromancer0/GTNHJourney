package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.research.ResearchKey;

public class ResearchMutationEngineTest {

    @Test
    public void deleteUndoRedoPreserveExactChronology() {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchKey a = new ResearchKey("minecraft:stone", 0, "");
        ResearchKey b = new ResearchKey("minecraft:dirt", 0, "");
        research.restoreEntry(player, new ResearchEntrySnapshot(a, null, 0));
        research.restoreEntry(player, new ResearchEntrySnapshot(b, null, 1));
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);

        assertTrue(engine.deleteExact(b, "D delete"));
        assertEquals(Arrays.asList(a), research.snapshotInUnlockOrder(player));
        assertEquals(1, recovery.undoDepth(player));

        assertEquals(1, engine.undo(1));
        assertEquals(Arrays.asList(a, b), research.snapshotInUnlockOrder(player));
        assertEquals(1, recovery.redoDepth(player));

        assertEquals(1, engine.redo(1));
        assertEquals(Arrays.asList(a), research.snapshotInUnlockOrder(player));
        assertEquals(1, recovery.undoDepth(player));
    }

    @Test
    public void bulkAddIsOneUndoableTransactionAndNoopCreatesNothing() {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);
        ResearchEntrySnapshot a = new ResearchEntrySnapshot(new ResearchKey("minecraft:stone", 0, ""), null, 0);
        ResearchEntrySnapshot b = new ResearchEntrySnapshot(new ResearchKey("minecraft:dirt", 0, ""), null, 1);

        assertEquals(2, engine.addEntries(Arrays.asList(a, b), "Migration AREA_16"));
        assertEquals(1, recovery.undoDepth(player));
        assertEquals(1, engine.undo(1));
        assertEquals(0, research.snapshotInUnlockOrder(player).size());

        assertEquals(0, engine.deleteExact(new ResearchKey("minecraft:glass", 0, ""), "missing") ? 1 : 0);
        assertEquals(0, recovery.undoDepth(player));
    }

    @Test
    public void bulkDeleteCreatesOneTransactionAndRestoresExactOrder() {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchKey a = new ResearchKey("minecraft:stone", 0, "");
        ResearchKey b = new ResearchKey("minecraft:dirt", 0, "");
        ResearchKey c = new ResearchKey("minecraft:glass", 0, "");
        research.restoreEntry(player, new ResearchEntrySnapshot(a, null, 0));
        research.restoreEntry(player, new ResearchEntrySnapshot(b, null, 1));
        research.restoreEntry(player, new ResearchEntrySnapshot(c, null, 2));
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);

        assertEquals(2, engine.deleteKeys(Arrays.asList(a, c), "Clear selected"));
        assertEquals(Arrays.asList(b), research.snapshotInUnlockOrder(player));
        assertEquals(1, recovery.undoDepth(player));
        assertEquals(2, recovery.activeDeletionCount(player));

        assertEquals(1, engine.undo(1));
        assertEquals(Arrays.asList(a, b, c), research.snapshotInUnlockOrder(player));
        assertEquals(0, recovery.activeDeletionCount(player));

        assertEquals(1, engine.redo(1));
        assertEquals(Arrays.asList(b), research.snapshotInUnlockOrder(player));
        assertEquals(2, recovery.activeDeletionCount(player));
    }

    @Test
    public void snapshotReplacementCarriesDeletionReconciliationThroughUndoRedo() throws Exception {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchKey a = new ResearchKey("minecraft:stone", 0, "");
        ResearchKey b = new ResearchKey("minecraft:dirt", 0, "");
        research.restoreEntry(player, new ResearchEntrySnapshot(a, null, 0));
        research.restoreEntry(player, new ResearchEntrySnapshot(b, null, 1));
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);

        assertTrue(engine.deleteExact(a, "D delete"));
        assertEquals(1, recovery.activeDeletionCount(player));
        DeletionRecord deletion = recovery.newestActiveDeletions(player, 1).get(0);

        ResearchStateSnapshot target = new ResearchStateSnapshot(
            Arrays.asList(
                new ResearchEntrySnapshot(a, null, 0),
                new ResearchEntrySnapshot(b, null, 1)));
        List<DeletionStateChange> changes = Collections.singletonList(new DeletionStateChange(deletion.id(), false));
        Method replace = assertDoesNotThrow(
            () -> ResearchMutationEngine.class.getDeclaredMethod(
                "replaceState",
                ResearchStateSnapshot.class,
                String.class,
                List.class));
        replace.setAccessible(true);

        assertEquals(2, ((Integer) replace.invoke(engine, target, "Restore snapshot", changes)).intValue());
        assertEquals(Arrays.asList(a, b), research.snapshotInUnlockOrder(player));
        assertEquals(0, recovery.activeDeletionCount(player));

        assertEquals(1, engine.undo(1));
        assertEquals(Arrays.asList(b), research.snapshotInUnlockOrder(player));
        assertEquals(1, recovery.activeDeletionCount(player));

        assertEquals(1, engine.redo(1));
        assertEquals(Arrays.asList(a, b), research.snapshotInUnlockOrder(player));
        assertEquals(0, recovery.activeDeletionCount(player));
    }

    @Test
    public void passiveMutationAfterUndoInvalidatesRedo() {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchKey a = new ResearchKey("minecraft:stone", 0, "");
        research.restoreEntry(player, new ResearchEntrySnapshot(a, null, 0));
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);

        assertTrue(engine.deleteExact(a, "D delete"));
        assertEquals(1, engine.undo(1));
        assertEquals(1, recovery.redoDepth(player));

        engine.notePassiveMutation();
        assertEquals(0, recovery.redoDepth(player));
        assertFalse(engine.redo(1) > 0);
    }
}
