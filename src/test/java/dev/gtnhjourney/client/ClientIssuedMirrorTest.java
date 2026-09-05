package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

public class ClientIssuedMirrorTest {

    @AfterEach
    public void clear() {
        ClientIssuedMirror.clear();
    }

    @Test
    public void incompleteIssuedEpochNeverReplacesVisibleOrder() {
        ResearchFingerprint old = fingerprint("old");
        ResearchFingerprint incoming = fingerprint("incoming");
        ClientIssuedMirror.touch(old);

        ClientIssuedMirror.begin(7, 2);
        ClientIssuedMirror.addChunk(7, Collections.singletonList(incoming));

        assertFalse(ClientIssuedMirror.isComplete(7));
        ClientIssuedMirror.abort(7);
        assertEquals(Collections.singletonList(old), ClientIssuedMirror.snapshotOldestFirst());
    }

    @Test
    public void completeIssuedEpochCanReorderTheSameMembership() {
        ResearchFingerprint first = fingerprint("first");
        ResearchFingerprint second = fingerprint("second");
        ClientIssuedMirror.touch(first);
        ClientIssuedMirror.touch(second);
        assertEquals(Arrays.asList(first, second), ClientIssuedMirror.snapshotOldestFirst());

        ClientIssuedMirror.begin(8, 2);
        ClientIssuedMirror.addChunk(8, Arrays.asList(second, first));
        assertTrue(ClientIssuedMirror.isComplete(8));
        ClientIssuedMirror.finish(8);

        assertEquals(Arrays.asList(second, first), ClientIssuedMirror.snapshotOldestFirst());
        assertTrue(ClientIssuedMirror.sequence(first) > ClientIssuedMirror.sequence(second));
    }

    @Test
    public void incrementalTouchMovesOnlyTheActuallyIssuedIdentity() {
        ResearchFingerprint first = fingerprint("first");
        ResearchFingerprint second = fingerprint("second");
        ClientIssuedMirror.touch(first);
        ClientIssuedMirror.touch(second);
        ClientIssuedMirror.touch(first);

        assertEquals(Arrays.asList(second, first), ClientIssuedMirror.snapshotOldestFirst());
        assertTrue(ClientIssuedMirror.sequence(first) > ClientIssuedMirror.sequence(second));
    }

    private static ResearchFingerprint fingerprint(String name) {
        return ResearchFingerprint.of(new ResearchKey("test:" + name, 0, ""));
    }
}
