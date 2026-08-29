package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class CommandHintDiagnosticsTest {

    @AfterEach
    public void reset() {
        CommandHintDiagnostics.resetForTests();
    }

    @Test
    public void snapshotTracksRegistrationResolverPathAndSuggestionCount() {
        CommandHintDiagnostics.markRegistered();
        CommandHintDiagnostics.recordResolverSuccess("example.GuiChat#input");
        CommandHintDiagnostics.recordSuggestionCount(7);

        CommandHintDiagnostics.Snapshot snapshot = CommandHintDiagnostics.snapshot();
        assertTrue(snapshot.registered());
        assertEquals("example.GuiChat#input", snapshot.resolverPath());
        assertEquals(0L, snapshot.resolverFailures());
        assertEquals(7, snapshot.lastSuggestionCount());
    }

    @Test
    public void failuresAreCountedWithoutLosingLastSuccessfulPath() {
        CommandHintDiagnostics.recordResolverSuccess("example.GuiChat#input");
        CommandHintDiagnostics.recordResolverFailure();
        CommandHintDiagnostics.recordResolverFailure();

        CommandHintDiagnostics.Snapshot snapshot = CommandHintDiagnostics.snapshot();
        assertFalse(snapshot.registered());
        assertEquals("example.GuiChat#input", snapshot.resolverPath());
        assertEquals(2L, snapshot.resolverFailures());
        assertEquals(0, snapshot.lastSuggestionCount());
    }
}
