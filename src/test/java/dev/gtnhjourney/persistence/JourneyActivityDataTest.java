package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;

public class JourneyActivityDataTest {

    @Test
    public void retrievalOrderSurvivesNbtRoundTripAndStillContainsTheFullResearchSet() {
        UUID player = new UUID(11L, 22L);
        ResearchKey log = key("log");
        ResearchKey stone = key("stone");
        ResearchKey potato = key("potato");
        List<ResearchKey> research = Arrays.asList(log, stone, potato);

        JourneyActivityData before = new JourneyActivityData("test_activity");
        before.recordUnlock(player, log);
        before.recordUnlock(player, stone);
        before.recordUnlock(player, potato);
        before.recordRetrieval(player, log);

        NBTTagCompound nbt = new NBTTagCompound();
        before.writeToNBT(nbt);

        JourneyActivityData after = new JourneyActivityData("test_activity");
        after.readFromNBT(nbt);

        assertEquals(Arrays.asList(stone, potato, log), after.snapshotReconciled(player, research));
    }

    @Test
    public void legacySaveWithoutActivitySeedsAllResearchWithoutDroppingAnything() {
        UUID player = new UUID(33L, 44L);
        ResearchKey log = key("log");
        ResearchKey stone = key("stone");
        ResearchKey potato = key("potato");

        JourneyActivityData data = new JourneyActivityData("test_activity");
        assertEquals(
            Arrays.asList(log, stone, potato),
            data.snapshotReconciled(player, Arrays.asList(log, stone, potato)));
    }

    private static ResearchKey key(String name) {
        return new ResearchKey("test:" + name, 0, "");
    }
}
