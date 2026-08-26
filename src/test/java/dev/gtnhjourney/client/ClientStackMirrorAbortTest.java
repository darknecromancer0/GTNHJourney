package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

public class ClientStackMirrorAbortTest {

    @AfterEach
    public void clear() {
        ClientStackMirror.clear();
    }

    @Test
    public void overlappingBeginAbortRestoresLastPublishedMetadata() {
        ClientStackMirror.clear();

        ClientStackMirror.begin(1, 5, 0);
        assertTrue(ClientStackMirror.finish(1));
        assertEquals(5, ClientStackMirror.serverAvailableTotal());
        assertEquals(0, ClientStackMirror.expectedSyncedTotal());
        assertEquals(5, ClientStackMirror.serverOnlyCount());

        ClientStackMirror.begin(2, 7, 0);
        ClientStackMirror.begin(3, 9, 0);
        ClientStackMirror.abort(3);

        assertEquals(5, ClientStackMirror.serverAvailableTotal());
        assertEquals(0, ClientStackMirror.expectedSyncedTotal());
        assertEquals(5, ClientStackMirror.serverOnlyCount());
    }

    @Test
    public void acknowledgedRemovalOfUnsyncedServerOnlyStateDecrementsServerOnlyCount() {
        ClientStackMirror.begin(10, 2, 0);
        assertTrue(ClientStackMirror.finish(10));
        assertEquals(2, ClientStackMirror.serverOnlyCount());

        ResearchFingerprint serverOnly = ResearchFingerprint.of(new ResearchKey("test:oversized", 0, "payload"));
        assertTrue(ClientStackMirror.remove(serverOnly));

        assertEquals(1, ClientStackMirror.serverAvailableTotal());
        assertEquals(0, ClientStackMirror.expectedSyncedTotal());
        assertEquals(1, ClientStackMirror.serverOnlyCount());
    }
}
