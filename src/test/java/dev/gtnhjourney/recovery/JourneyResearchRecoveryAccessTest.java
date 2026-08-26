package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.persistence.JourneyRecoveryData;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.research.ResearchKey;

public class JourneyResearchRecoveryAccessTest {

    @Test
    public void exactRemoveAndRestorePreserveOriginalTimelinePosition() {
        UUID player = UUID.randomUUID();
        JourneyResearchData data = new JourneyResearchData();
        ResearchKey a = new ResearchKey("minecraft:stone", 0, "");
        ResearchKey b = new ResearchKey("minecraft:dirt", 0, "{x:1}");
        ResearchKey c = new ResearchKey("minecraft:glass", 0, "");
        NBTTagCompound bTag = new NBTTagCompound();
        bTag.setInteger("x", 1);

        assertTrue(data.restoreEntry(player, new ResearchEntrySnapshot(a, null, 0)));
        assertTrue(data.restoreEntry(player, new ResearchEntrySnapshot(b, bTag, 1)));
        assertTrue(data.restoreEntry(player, new ResearchEntrySnapshot(c, null, 2)));
        assertEquals(Arrays.asList(a, b, c), data.snapshotInUnlockOrder(player));

        ResearchEntrySnapshot removed = data.removeEntry(player, b);
        assertNotNull(removed);
        assertEquals(1, removed.timelineIndex());
        assertEquals(1, removed.template().getInteger("x"));
        assertEquals(Arrays.asList(a, c), data.snapshotInUnlockOrder(player));

        assertTrue(data.restoreEntry(player, removed));
        assertEquals(Arrays.asList(a, b, c), data.snapshotInUnlockOrder(player));
        assertFalse(data.restoreEntry(player, removed));
    }

    @Test
    public void snapshotRestorabilityRejectsMissingItemsBeforeMutation() throws Exception {
        Method validator = assertDoesNotThrow(
            () -> JourneyMutationService.class.getDeclaredMethod("isRestorableState", ResearchStateSnapshot.class));
        validator.setAccessible(true);

        assertTrue((Boolean) validator.invoke(null, new ResearchStateSnapshot(Collections.<ResearchEntrySnapshot>emptyList())));

        ResearchKey missing = new ResearchKey("journey_missing_mod:ghost", 0, "");
        ResearchStateSnapshot broken = new ResearchStateSnapshot(
            Collections.singletonList(new ResearchEntrySnapshot(missing, null, 0)));
        assertFalse((Boolean) validator.invoke(null, broken));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void snapshotDeletionPlanDeactivatesOnlyActiveRecordsWhoseKeysArePresentInTarget() throws Exception {
        UUID player = UUID.randomUUID();
        ResearchKey present = new ResearchKey("minecraft:stone", 0, "");
        ResearchKey absent = new ResearchKey("minecraft:dirt", 0, "");
        JourneyRecoveryData recovery = new JourneyRecoveryData();
        recovery.appendDeletion(player, new DeletionRecord(101L, 1L, new ResearchEntrySnapshot(present, null, 0), true));
        recovery.appendDeletion(player, new DeletionRecord(102L, 2L, new ResearchEntrySnapshot(absent, null, 1), true));

        ResearchStateSnapshot target = new ResearchStateSnapshot(
            Collections.singletonList(new ResearchEntrySnapshot(present, null, 0)));
        Method planner = assertDoesNotThrow(
            () -> JourneyMutationService.class.getDeclaredMethod(
                "deletionChangesForTarget",
                JourneyRecoveryData.class,
                UUID.class,
                ResearchStateSnapshot.class));
        planner.setAccessible(true);

        List<DeletionStateChange> changes = (List<DeletionStateChange>) planner.invoke(null, recovery, player, target);
        assertEquals(1, changes.size());
        assertEquals(101L, changes.get(0).deletionId());
        assertFalse(changes.get(0).activeAfterForward());
        assertEquals(2, recovery.activeDeletionCount(player));
    }
}
