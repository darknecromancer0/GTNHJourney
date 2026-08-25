package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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
