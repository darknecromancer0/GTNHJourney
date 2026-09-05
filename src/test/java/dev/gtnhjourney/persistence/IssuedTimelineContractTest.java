package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.nei.JourneyOrderMode;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;

/** Contract for a truthful issuance-only chronology, independent from research/unlock activity. */
public class IssuedTimelineContractTest {

    @SuppressWarnings("unchecked")
    @Test
    public void unlockDoesNotEnterIssuedTimelineButRetrievalDoesAndPersists() throws Exception {
        UUID player = new UUID(77L, 88L);
        ResearchKey unlockedOnly = key("unlocked_only");
        ResearchKey issued = key("issued");

        JourneyActivityData before = new JourneyActivityData("test_activity");
        before.recordUnlock(player, unlockedOnly);
        before.recordUnlock(player, issued);
        before.recordRetrieval(player, issued);

        Method snapshotIssued = JourneyActivityData.class.getMethod("snapshotIssuedOldestFirst", UUID.class);
        List<ResearchKey> live = (List<ResearchKey>) snapshotIssued.invoke(before, player);
        assertEquals(Arrays.asList(issued), live);

        NBTTagCompound nbt = new NBTTagCompound();
        before.writeToNBT(nbt);
        JourneyActivityData after = new JourneyActivityData("test_activity");
        after.readFromNBT(nbt);
        List<ResearchKey> restored = (List<ResearchKey>) snapshotIssued.invoke(after, player);
        assertEquals(Arrays.asList(issued), restored);
    }

    @Test
    public void issuedIsAnExplicitOrderModeRatherThanChangingLegacyLatest() {
        assertEquals("ISSUED", JourneyOrderMode.valueOf("ISSUED").name());
        assertTrue(JourneyOrderMode.valueOf("ISSUED").label().toLowerCase().contains("issued"));
    }

    private static ResearchKey key(String name) {
        return new ResearchKey("test:" + name, 0, "");
    }
}
