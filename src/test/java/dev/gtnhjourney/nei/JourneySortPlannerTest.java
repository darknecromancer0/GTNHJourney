package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class JourneySortPlannerTest {

    @Test
    public void nativeLatestMovesWholeFluidFamilyWithoutBreakingNativeMemberOrder() {
        JourneySortEntry batteryA = entry("mod:battery", 0, 0, "battery", 1, 10, 50, "Battery A");
        JourneySortEntry fluidEmpty = entry("mod:cell", 0, 1, "fluid", 2, 20, 30, "Empty Cell");
        JourneySortEntry fluidWater = entry("mod:cell", 1, 2, "fluid", 3, 30, 100, "Water Cell");
        JourneySortEntry batteryB = entry("mod:battery", 1, 3, "battery", 4, 40, 60, "Battery B");

        List<JourneySortEntry> result = JourneySortPlanner.sort(
            Arrays.asList(batteryA, fluidEmpty, fluidWater, batteryB),
            JourneyGroupMode.NATIVE,
            JourneyOrderMode.NONE,
            true);

        assertEquals(Arrays.asList(fluidEmpty, fluidWater, batteryA, batteryB), result);
    }

    @Test
    public void disablingNativeGroupLeavesPureLatestWithNoResidualFamilyOrdering() {
        JourneySortEntry fluidEmpty = entry("mod:cell", 0, 1, "fluid", 2, 20, 30, "Empty Cell");
        JourneySortEntry fluidWater = entry("mod:cell", 1, 2, "fluid", 3, 30, 100, "Water Cell");
        JourneySortEntry battery = entry("mod:battery", 0, 0, "battery", 1, 10, 80, "Battery");

        List<JourneySortEntry> result = JourneySortPlanner.sort(
            Arrays.asList(fluidEmpty, fluidWater, battery),
            JourneyGroupMode.NONE,
            JourneyOrderMode.NONE,
            true);

        assertEquals(Arrays.asList(fluidWater, battery, fluidEmpty), result);
    }

    @Test
    public void nativeUnlockRanksWholeFamilyByNewestLearnedMember() {
        JourneySortEntry fluidOld = entry("mod:cell", 0, 1, "fluid", 10, 1, 0, "Empty Cell");
        JourneySortEntry fluidNew = entry("mod:cell", 1, 2, "fluid", 100, 2, 0, "Oxygen Cell");
        JourneySortEntry battery = entry("mod:battery", 0, 0, "battery", 90, 0, 0, "Battery");

        List<JourneySortEntry> result = JourneySortPlanner.sort(
            Arrays.asList(battery, fluidOld, fluidNew),
            JourneyGroupMode.NATIVE,
            JourneyOrderMode.UNLOCK,
            false);

        assertEquals(Arrays.asList(fluidOld, fluidNew, battery), result);
    }

    @Test
    public void latestIsPrimaryAndUnlockIsSecondaryForGroupedSort() {
        JourneySortEntry fluid = entry("mod:cell", 0, 2, "fluid", 100, 0, 50, "Cell");
        JourneySortEntry battery = entry("mod:battery", 0, 0, "battery", 200, 0, 80, "Battery");
        JourneySortEntry tool = entry("mod:tool", 0, 1, "tool", 300, 0, 80, "Tool");

        List<JourneySortEntry> result = JourneySortPlanner.sort(
            Arrays.asList(fluid, battery, tool), JourneyGroupMode.NATIVE, JourneyOrderMode.UNLOCK, true);

        assertEquals(Arrays.asList(tool, battery, fluid), result);
    }

    @Test
    public void alphabeticalWithoutGroupingSortsIndividualStates() {
        JourneySortEntry z = entry("mod:z", 0, 0, "z", 0, 0, 0, "Zulu");
        JourneySortEntry a = entry("mod:a", 0, 1, "a", 0, 0, 0, "Alpha");
        assertEquals(
            Arrays.asList(a, z),
            JourneySortPlanner.sort(Arrays.asList(z, a), JourneyGroupMode.NONE, JourneyOrderMode.ALPHABETICAL, false));
    }

    private static JourneySortEntry entry(
        String id,
        int meta,
        int nativeIndex,
        String family,
        long unlock,
        long favourite,
        long activity,
        String name) {
        return new JourneySortEntry(
            new ResearchKey(id, meta, ""),
            null,
            nativeIndex,
            family,
            id.substring(0, id.indexOf(':')),
            "misc",
            family,
            name,
            unlock,
            activity,
            favourite,
            nativeIndex);
    }
}
