package dev.gtnhjourney.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SemanticDiagnosticSnapshotTest {

    @Test
    public void inspectSummaryReportsEverySupportedSemanticPolicy() {
        SemanticDiagnosticSnapshot snapshot = new SemanticDiagnosticSnapshot(
            "BASE",
            "EXACT",
            "FULL",
            "EXACT",
            true,
            false,
            true,
            false,
            2);

        assertEquals(
            "Semantic: GT-charge=BASE, IC2-charge=EXACT, CoFH-charge=FULL, OC-charge=EXACT, GT-tool=true, "
                + "TCon-tool=false, Botania-magnet=true, Draconic-tool=false, observed endpoints=2",
            snapshot.inspectLine());
        assertEquals("GT-charge, CoFH-charge, GT-tool, Botania-magnet", snapshot.matchedPoliciesCsv());
    }

    @Test
    public void exactNbtWithoutKnownSemanticOwnerIsFlaggedUnknown() {
        SemanticDiagnosticSnapshot unknown = new SemanticDiagnosticSnapshot(
            "EXACT",
            "EXACT",
            "EXACT",
            "EXACT",
            false,
            false,
            false,
            false,
            1);

        assertTrue(unknown.isUnknownExactNbt(true));
        assertFalse(unknown.isUnknownExactNbt(false));
    }

    @Test
    public void knownSemanticOwnerPreventsUnknownExactNbtClassification() {
        SemanticDiagnosticSnapshot knownTool = new SemanticDiagnosticSnapshot(
            "EXACT",
            "EXACT",
            "EXACT",
            "EXACT",
            false,
            true,
            false,
            false,
            1);

        assertFalse(knownTool.isUnknownExactNbt(true));
    }
}
