package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ItemDebugResearcherToolInteractionTest {

    @Test
    public void shiftRightClickAlwaysCyclesAndConsumesBeforeAnyBlockGui() {
        for (DebugResearchMode mode : DebugResearchMode.values()) {
            assertDecision(
                ItemDebugResearcherTool.InteractionAction.CYCLE_MODE,
                ItemDebugResearcherTool.route(mode, true, true));
            assertDecision(
                ItemDebugResearcherTool.InteractionAction.CYCLE_MODE,
                ItemDebugResearcherTool.route(mode, true, false));
        }
    }

    @Test
    public void targetedNormalRightClickExecutesEveryModeAndConsumes() {
        for (DebugResearchMode mode : DebugResearchMode.values()) {
            assertDecision(
                ItemDebugResearcherTool.InteractionAction.EXECUTE,
                ItemDebugResearcherTool.route(mode, false, true));
        }
    }

    @Test
    public void airRightClickExecutesOnlyArea16AndConsumesNoTargetForOtherModes() {
        assertDecision(
            ItemDebugResearcherTool.InteractionAction.NO_TARGET,
            ItemDebugResearcherTool.route(DebugResearchMode.BLOCK, false, false));
        assertDecision(
            ItemDebugResearcherTool.InteractionAction.NO_TARGET,
            ItemDebugResearcherTool.route(DebugResearchMode.CONTENTS, false, false));
        assertDecision(
            ItemDebugResearcherTool.InteractionAction.EXECUTE,
            ItemDebugResearcherTool.route(DebugResearchMode.AREA_16, false, false));
    }

    private static void assertDecision(
        ItemDebugResearcherTool.InteractionAction expected,
        ItemDebugResearcherTool.InteractionDecision actual) {
        assertEquals(expected, actual.action());
        assertTrue(actual.consumed());
    }
}
