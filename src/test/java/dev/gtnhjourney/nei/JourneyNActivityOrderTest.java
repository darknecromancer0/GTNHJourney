package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class JourneyNActivityOrderTest {

    @Test
    public void nContainsSameResearchSetAsJButUsesActivityOrder() {
        ResearchKey log = key("log");
        ResearchKey stone = key("stone");
        ResearchKey potato = key("potato");
        List<ResearchKey> researchOldestFirst = Arrays.asList(log, stone, potato);
        List<ResearchKey> activityOldestFirst = Arrays.asList(stone, potato, log);

        assertEquals(
            Arrays.asList(potato, stone, log),
            JourneyPanelOrder.keysForMode(
                researchOldestFirst,
                activityOldestFirst,
                JourneyViewState.Mode.RESEARCHED));
        assertEquals(
            Arrays.asList(log, potato, stone),
            JourneyPanelOrder.keysForMode(
                researchOldestFirst,
                activityOldestFirst,
                JourneyViewState.Mode.NEWEST));
    }

    @Test
    public void nAppendsAnyMissingResearchDefensivelyInsteadOfDroppingIt() {
        ResearchKey log = key("log");
        ResearchKey stone = key("stone");
        ResearchKey potato = key("potato");

        assertEquals(
            Arrays.asList(stone, log, potato),
            JourneyPanelOrder.keysForMode(
                Arrays.asList(log, stone, potato),
                Arrays.asList(log, stone),
                JourneyViewState.Mode.NEWEST));
    }

    private static ResearchKey key(String name) {
        return new ResearchKey("test:" + name, 0, "");
    }
}
