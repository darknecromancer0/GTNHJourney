package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.research.ResearchKey;

public class SnapshotRestoreTransactionTest {

    @Test
    public void replaceStateUndoRedoRestoresExactOrderAndTemplates() {
        UUID player = UUID.randomUUID();
        JourneyResearchData research = new JourneyResearchData();
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        ResearchMutationEngine engine = new ResearchMutationEngine(research, recovery, player);

        ResearchStateSnapshot before = state(entry("minecraft:stone", 0, "old"), entry("minecraft:dirt", 1, "old2"));
        for (ResearchEntrySnapshot entry : before.entries()) research.restoreEntry(player, entry);

        ResearchStateSnapshot target = state(entry("minecraft:glass", 0, "new"), entry("IC2:itemFluidCell", 1, "water"));
        assertEquals(2, engine.replaceState(target, "Restore snapshot"));
        assertState(target, research.captureState(player));

        assertEquals(1, engine.undo(1));
        assertState(before, research.captureState(player));

        assertEquals(1, engine.redo(1));
        assertState(target, research.captureState(player));
    }

    private static ResearchEntrySnapshot entry(String id, int index, String marker) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("marker", marker);
        return new ResearchEntrySnapshot(new ResearchKey(id, 0, ""), tag, index);
    }

    private static ResearchStateSnapshot state(ResearchEntrySnapshot... entries) {
        return new ResearchStateSnapshot(Arrays.asList(entries));
    }

    private static void assertState(ResearchStateSnapshot expected, ResearchStateSnapshot actual) {
        assertEquals(expected.size(), actual.size());
        List<ResearchEntrySnapshot> left = expected.entries();
        List<ResearchEntrySnapshot> right = actual.entries();
        for (int i = 0; i < left.size(); i++) {
            assertEquals(left.get(i).key(), right.get(i).key());
            assertEquals(left.get(i).timelineIndex(), right.get(i).timelineIndex());
            assertEquals(left.get(i).template().getString("marker"), right.get(i).template().getString("marker"));
        }
    }
}
