package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JourneyCommandErrorPolicyTest {

    @Test
    void invalidRootSuggestsOnlyTopLevelUsageInsteadOfFullHelp() {
        assertEquals(
            "Invalid Journey command 'wat'. Try: /journey [help|count|stats|inspect|research|rescan|list|newest|get|forget|undo|redo|restore-deleted|snapshot|snapshots|restore|backup|explosions|cleanse|speed|botania|debug|trace|dump|hotspots|debugtool|prune-missing|clear|return|death]",
            JourneyCommandErrorPolicy.invalidRoot("wat"));
    }

    @Test
    void invalidExplosionBranchShowsExplosionChoicesOnly() {
        assertEquals(
            "Invalid explosions command 'ogf'. Try: /journey explosions [status|on|off|default|undo|redo|machines]",
            JourneyCommandErrorPolicy.invalid("explosions", "ogf", "status|on|off|default|undo|redo|machines"));
    }

    @Test
    void invalidMachineExplosionValueShowsDeepChoicesOnly() {
        assertEquals(
            "Invalid explosions machines command 'wat'. Try: /journey explosions machines [status|on|off]",
            JourneyCommandErrorPolicy.invalid("explosions machines", "wat", "status|on|off"));
    }
}
