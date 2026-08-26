package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;

public class ResearchMutationConflictTest {

    @Test
    public void undoDoesNotMoveTransactionWhenDeletedStateWasReacquiredDifferently() {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchKey victim = new ResearchKey("test:victim", 0, "");
        ResearchKey other = new ResearchKey("test:other", 0, "");

        NBTTagCompound original = new NBTTagCompound();
        original.setString("Variant", "original");
        NBTTagCompound reacquired = new NBTTagCompound();
        reacquired.setString("Variant", "reacquired");

        research.restoreEntry(player, new ResearchEntrySnapshot(victim, original, 0));
        research.restoreEntry(player, new ResearchEntrySnapshot(other, null, 1));
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);

        assertTrue(engine.deleteExact(victim, "D delete"));
        assertTrue(research.restoreEntry(player, new ResearchEntrySnapshot(victim, reacquired, 1)));
        engine.notePassivePresent(victim);
        assertEquals(Arrays.asList(other, victim), research.snapshotInUnlockOrder(player));
        assertEquals("reacquired", research.template(player, victim).getString("Variant"));

        assertEquals(0, engine.undo(1));
        assertEquals(Arrays.asList(other, victim), research.snapshotInUnlockOrder(player));
        assertEquals("reacquired", research.template(player, victim).getString("Variant"));
        assertEquals(1, recovery.undoDepth(player));
        assertEquals(0, recovery.redoDepth(player));
    }

    @Test
    public void redoDoesNotDeleteStateThatNoLongerMatchesItsForwardRemovalSnapshot() {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchKey victim = new ResearchKey("test:victim", 0, "");

        NBTTagCompound original = new NBTTagCompound();
        original.setString("Variant", "original");
        NBTTagCompound diverged = new NBTTagCompound();
        diverged.setString("Variant", "diverged");

        research.restoreEntry(player, new ResearchEntrySnapshot(victim, original, 0));
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);
        assertTrue(engine.deleteExact(victim, "D delete"));
        assertEquals(1, engine.undo(1));
        assertEquals(1, recovery.redoDepth(player));

        assertTrue(research.removeEntry(player, victim) != null);
        assertTrue(research.restoreEntry(player, new ResearchEntrySnapshot(victim, diverged, 0)));
        assertEquals("diverged", research.template(player, victim).getString("Variant"));

        assertEquals(0, engine.redo(1));
        assertTrue(research.registry(player).contains(victim));
        assertEquals("diverged", research.template(player, victim).getString("Variant"));
        assertEquals(0, recovery.undoDepth(player));
        assertEquals(1, recovery.redoDepth(player));
    }
}
