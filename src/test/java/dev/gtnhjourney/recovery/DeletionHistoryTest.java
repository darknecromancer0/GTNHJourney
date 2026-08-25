package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.research.ResearchKey;

public class DeletionHistoryTest {

    @Test
    public void newestActiveRecordsAreSelectedAndPresenceTogglesActivity() {
        UUID player = UUID.randomUUID();
        JourneyRecoveryData data = new JourneyRecoveryData();
        ResearchEntrySnapshot stone = entry("minecraft:stone", 0);
        ResearchEntrySnapshot dirt = entry("minecraft:dirt", 1);
        ResearchEntrySnapshot glass = entry("minecraft:glass", 2);

        data.appendDeletion(player, new DeletionRecord(1L, 10L, stone, true));
        data.appendDeletion(player, new DeletionRecord(2L, 20L, dirt, true));
        data.appendDeletion(player, new DeletionRecord(3L, 30L, glass, true));

        List<DeletionRecord> newest = data.newestActiveDeletions(player, 2);
        assertEquals(2, newest.size());
        assertEquals(glass.key(), newest.get(0).entry().key());
        assertEquals(dirt.key(), newest.get(1).entry().key());

        assertTrue(data.markDeletionInactiveForPresentKey(player, glass.key()));
        assertFalse(data.newestActiveDeletions(player, 10).get(0).entry().key().equals(glass.key()));
        assertTrue(data.markNewestDeletionActiveForAbsentKey(player, glass.key()));
        assertEquals(glass.key(), data.newestActiveDeletions(player, 1).get(0).entry().key());
    }

    @Test
    public void deleteUndoRedoAndRestoreDeletedKeepHistoryCoherent() {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchEntrySnapshot stone = entry("minecraft:stone", 0);
        research.restoreEntry(player, stone);
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);

        assertTrue(engine.deleteExact(stone.key(), "D delete"));
        assertEquals(1, recovery.activeDeletionCount(player));

        assertEquals(1, engine.undo(1));
        assertEquals(0, recovery.activeDeletionCount(player));

        assertEquals(1, engine.redo(1));
        assertEquals(1, recovery.activeDeletionCount(player));

        assertEquals(1, engine.restoreDeleted(1));
        assertEquals(0, recovery.activeDeletionCount(player));
        assertEquals(1, engine.undo(1));
        assertEquals(1, recovery.activeDeletionCount(player));
        assertEquals(1, engine.redo(1));
        assertEquals(0, recovery.activeDeletionCount(player));
    }

    @Test
    public void deletionHistorySurvivesNbtRoundTrip() {
        UUID player = UUID.randomUUID();
        JourneyRecoveryData original = new JourneyRecoveryData();
        ResearchEntrySnapshot cell = entry("IC2:itemFluidCell", 4);
        original.appendDeletion(player, new DeletionRecord(77L, 1234L, cell, true));

        NBTTagCompound root = new NBTTagCompound();
        original.writeToNBT(root);

        JourneyRecoveryData restored = new JourneyRecoveryData();
        restored.readFromNBT(root);
        List<DeletionRecord> active = restored.newestActiveDeletions(player, 10);

        assertEquals(1, active.size());
        assertEquals(77L, active.get(0).id());
        assertEquals(cell.key(), active.get(0).entry().key());
        assertTrue(active.get(0).active());
    }

    private static ResearchEntrySnapshot entry(String itemId, int index) {
        return new ResearchEntrySnapshot(new ResearchKey(itemId, 0, ""), null, index);
    }
}
