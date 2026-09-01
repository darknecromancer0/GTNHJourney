package dev.gtnhjourney.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyDisasterRecoveryDiagnosticsContractTest {

    @Test
    void dumpProvesExternalRecoveryAndNativeBackupOwnership() throws Exception {
        String dump = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/diagnostics/JourneyDiagnosticDump.java")),
            StandardCharsets.UTF_8);
        String ticker = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/recovery/JourneySnapshotTicker.java")),
            StandardCharsets.UTF_8);

        assertTrue(dump.contains("externalSnapshotSuccesses="));
        assertTrue(dump.contains("externalSnapshotFailures="));
        assertTrue(dump.contains("externalSnapshotLastPath="));
        assertTrue(dump.contains("externalSnapshotLastBytes="));
        assertTrue(dump.contains("externalSnapshotLastResearchEntries="));
        assertTrue(dump.contains("externalSnapshotLastInventoryEntries="));
        assertTrue(dump.contains("nativeBackupOwnerActive="));
        assertTrue(ticker.contains("externalSnapshotSuccesses"));
        assertTrue(ticker.contains("lastExternalSnapshotPath"));
        assertTrue(ticker.contains("lastExternalSnapshotInventoryEntries"));
    }
}
