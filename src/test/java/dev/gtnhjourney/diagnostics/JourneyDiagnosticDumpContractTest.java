package dev.gtnhjourney.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class JourneyDiagnosticDumpContractTest {

    @Test
    public void expandedSnapshotEmitsRequiredIntegrityAndClientFields() {
        JourneyDiagnosticSnapshot snapshot = new JourneyDiagnosticSnapshot(
            11,
            15,
            13,
            2,
            "RESEARCHED",
            "lithium",
            Arrays.asList("example.SearchProvider"),
            15,
            14,
            11,
            true,
            "GuiChat#inputField",
            3L,
            5,
            true,
            "Backup completed: test.zip",
            1234L);

        String joined = join(snapshot.lines());
        assertContains(joined, "clientMirrorStacks=11");
        assertContains(joined, "serverAuthoritativeResearch=15");
        assertContains(joined, "serverSyncable=13");
        assertContains(joined, "serverOnlyOversized=2");
        assertContains(joined, "journeyMode=RESEARCHED");
        assertContains(joined, "neiSearchText=lithium");
        assertContains(joined, "journeyAppliedFilterProviders=1");
        assertContains(joined, "panelAuthoritativeStacks=15");
        assertContains(joined, "panelSemanticStacks=14");
        assertContains(joined, "panelVisibleStacks=11");
        assertContains(joined, "commandHintsRegistered=true");
        assertContains(joined, "commandHintResolverPath=GuiChat#inputField");
        assertContains(joined, "commandHintResolverFailures=3");
        assertContains(joined, "commandHintLastSuggestionCount=5");
        assertContains(joined, "backupRunning=true");
        assertContains(joined, "backupLastResult=Backup completed: test.zip");
        assertContains(joined, "backupLastDurationMillis=1234");
    }

    @Test
    public void researchDisplayNameHelperNeverThrowsForUnavailableEntry() {
        assertTrue(JourneyDiagnosticDump.safeDisplayName(null).startsWith("UNAVAILABLE"));
    }

    private static String join(List<String> lines) {
        StringBuilder out = new StringBuilder();
        for (String line : lines) out.append(line).append('\n');
        return out.toString();
    }

    private static void assertContains(String text, String expected) {
        assertTrue(text.contains(expected), "missing: " + expected + "\n" + text);
    }
}
