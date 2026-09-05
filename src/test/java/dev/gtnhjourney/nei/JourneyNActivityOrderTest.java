package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class JourneyNActivityOrderTest {

    @Test
    public void latestContainsSameResearchSetAsJButUsesActivityOrder() {
        JourneySortEntry log = entry("log", 1, 30);
        JourneySortEntry stone = entry("stone", 2, 10);
        JourneySortEntry potato = entry("potato", 3, 20);

        List<JourneySortEntry> result = JourneySortPlanner.sort(
            Arrays.asList(log, stone, potato), JourneyGroupMode.NONE, JourneyOrderMode.NONE, true);

        assertEquals(Arrays.asList(log, potato, stone), result);
    }

    @Test
    public void latestKeepsItemsWithoutActivityAfterKnownActivityInsteadOfDroppingThem() {
        JourneySortEntry log = entry("log", 1, 10);
        JourneySortEntry stone = entry("stone", 2, 20);
        JourneySortEntry potato = entry("potato", 3, -1);

        List<JourneySortEntry> result = JourneySortPlanner.sort(
            Arrays.asList(log, stone, potato), JourneyGroupMode.NONE, JourneyOrderMode.NONE, true);

        assertEquals(Arrays.asList(stone, log, potato), result);
    }

    private static JourneySortEntry entry(String name, int canonical, long activity) {
        ResearchKey key = new ResearchKey("test:" + name, 0, "");
        return new JourneySortEntry(
            key,
            null,
            canonical,
            key.getItemId(),
            "test",
            "misc",
            "misc",
            name,
            canonical,
            activity,
            -1L,
            canonical);
    }
}
