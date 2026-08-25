package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

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
}
