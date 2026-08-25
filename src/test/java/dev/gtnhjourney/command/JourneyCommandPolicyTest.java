package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JourneyCommandPolicyTest {

    @Test
    public void undoRedoCountsDefaultAndClampToOneThroughOneHundred() {
        assertEquals(1, JourneyCommandPolicy.parseUndoRedoCount(null));
        assertEquals(1, JourneyCommandPolicy.parseUndoRedoCount("0"));
        assertEquals(7, JourneyCommandPolicy.parseUndoRedoCount("7"));
        assertEquals(100, JourneyCommandPolicy.parseUndoRedoCount("999"));
    }

    @Test
    public void restoreDeletedClampsToOneThroughOneThousand() {
        assertEquals(1, JourneyCommandPolicy.parseRestoreDeletedCount(null));
        assertEquals(1, JourneyCommandPolicy.parseRestoreDeletedCount("-5"));
        assertEquals(250, JourneyCommandPolicy.parseRestoreDeletedCount("250"));
        assertEquals(1000, JourneyCommandPolicy.parseRestoreDeletedCount("5000"));
    }

    @Test
    public void invalidCountsFallBackToDefaultOneInsteadOfMutatingUnexpectedly() {
        assertEquals(1, JourneyCommandPolicy.parseUndoRedoCount("not-a-number"));
        assertEquals(1, JourneyCommandPolicy.parseRestoreDeletedCount(""));
    }
}
