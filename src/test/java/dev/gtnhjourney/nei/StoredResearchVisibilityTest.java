package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class StoredResearchVisibilityTest {

    @Test
    public void researchedPanelIncludesStoredDrillAndFilledCellKeysNewestFirstWithoutNativeNeiPredicate() {
        ResearchKey drillBase = new ResearchKey("IC2:itemToolDrill", 26, "");
        ResearchKey drillFull = new ResearchKey("IC2:itemToolDrill", 1, "10{6:charge=6:30000.0d;}");
        ResearchKey filledCell = new ResearchKey(
            "IC2:itemFluidCell",
            0,
            "10{5:Fluid=10{6:Amount=3:1000;9:FluidName=8:\"molten.orundum\";};}");

        List<ResearchKey> oldestFirst = Arrays.asList(drillBase, drillFull, filledCell);

        assertEquals(
            Arrays.asList(filledCell, drillFull, drillBase),
            JourneyPanelSnapshot.keys(oldestFirst, JourneyViewState.Mode.RESEARCHED, 64));
    }

    @Test
    public void nFallbackKeepsEveryStoredResearchStateWhenNoSeparateActivitySnapshotIsAvailable() {
        ResearchKey drillBase = new ResearchKey("IC2:itemToolDrill", 26, "");
        ResearchKey drillFull = new ResearchKey("IC2:itemToolDrill", 1, "10{6:charge=6:30000.0d;}");
        ResearchKey filledCell = new ResearchKey(
            "IC2:itemFluidCell",
            0,
            "10{5:Fluid=10{6:Amount=3:1000;9:FluidName=8:\"molten.orundum\";};}");

        assertEquals(
            Arrays.asList(filledCell, drillFull, drillBase),
            JourneyPanelSnapshot.keys(
                Arrays.asList(drillBase, drillFull, filledCell),
                JourneyViewState.Mode.NEWEST,
                2));
    }
}
