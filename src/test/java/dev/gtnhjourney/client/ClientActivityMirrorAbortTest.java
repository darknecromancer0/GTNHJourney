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

public class ClientActivityMirrorAbortTest {

    @AfterEach
    public void clear() {
        ClientActivityMirror.clear();
    }

    @Test
    public void abortDropsStagedActivityWithoutReplacingVisibleOrder() {
        ResearchKey old = key("old");
        ResearchKey incoming = key("incoming");
        ClientActivityMirror.recordUnlock(old);

        ClientActivityMirror.begin(7);
        ClientActivityMirror.addChunk(7, Collections.singletonList(ResearchFingerprint.of(incoming)));
        ClientActivityMirror.abort(7);

        assertEquals(Collections.singletonList(old), ClientActivityMirror.snapshotOldestFirst());
    }

    @Test
    public void staleAbortCannotCancelANewerEpoch() {
        ResearchKey first = key("first");
        ResearchKey second = key("second");

        ClientActivityMirror.begin(10);
        ClientActivityMirror.begin(11);
        ClientActivityMirror.addChunk(
            11,
            Arrays.asList(ResearchFingerprint.of(first), ResearchFingerprint.of(second)));
        ClientActivityMirror.abort(10);
        ClientActivityMirror.finish(11, Arrays.asList(first, second));

        assertEquals(Arrays.asList(first, second), ClientActivityMirror.snapshotOldestFirst());
    }

    @Test
    public void freshUnlockEventMovesAStaleEntryToNewest() {
        ResearchKey first = key("first");
        ResearchKey second = key("second");
        ClientActivityMirror.recordUnlock(first);
        ClientActivityMirror.recordUnlock(second);

        ClientActivityMirror.recordUnlock(first);

        assertEquals(Arrays.asList(second, first), ClientActivityMirror.snapshotOldestFirst());
    }

    @Test
    public void incompleteActivityEpochIsRejectedBeforeVisibleOrderCanBeReplaced() {
        ResearchKey first = key("first");
        ResearchKey second = key("second");
        ClientActivityMirror.recordUnlock(first);
        ClientActivityMirror.recordUnlock(second);

        ClientActivityMirror.begin(12, 2);
        ClientActivityMirror.addChunk(12, Collections.singletonList(ResearchFingerprint.of(first)));

        assertFalse(ClientActivityMirror.isComplete(12));
        ClientActivityMirror.abort(12);
        assertEquals(Arrays.asList(first, second), ClientActivityMirror.snapshotOldestFirst());
    }

    @Test
    public void completeActivityEpochPassesExpectedCountGate() {
        ResearchKey first = key("first");
        ResearchKey second = key("second");

        ClientActivityMirror.begin(13, 2);
        ClientActivityMirror.addChunk(
            13,
            Arrays.asList(ResearchFingerprint.of(first), ResearchFingerprint.of(second)));

        assertTrue(ClientActivityMirror.isComplete(13));
        ClientActivityMirror.finish(13, Arrays.asList(first, second));
        assertEquals(Arrays.asList(first, second), ClientActivityMirror.snapshotOldestFirst());
    }

    @Test
    public void completeActivityCountMayIncludeServerOnlyKeyWithoutChangingVisibleMembership() {
        ResearchKey first = key("first");
        ResearchKey second = key("second");
        ResearchKey serverOnly = key("serverOnly");

        ClientActivityMirror.begin(14, 3);
        ClientActivityMirror.addChunk(
            14,
            Arrays.asList(
                ResearchFingerprint.of(first),
                ResearchFingerprint.of(serverOnly),
                ResearchFingerprint.of(second)));

        assertTrue(ClientActivityMirror.isComplete(14));
        ClientActivityMirror.finish(14, Arrays.asList(first, second));
        assertEquals(Arrays.asList(first, second), ClientActivityMirror.snapshotOldestFirst());
    }

    private static ResearchKey key(String name) {
        return new ResearchKey("test:" + name, 0, "");
    }
}
