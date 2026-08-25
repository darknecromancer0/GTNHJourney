package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class ResearchTransactionTest {

    @Test
    public void transactionCarriesExactAddedAndRemovedDeltas() {
        ResearchEntrySnapshot added = new ResearchEntrySnapshot(new ResearchKey("minecraft:stone", 0, ""), null, 2);
        ResearchEntrySnapshot removed = new ResearchEntrySnapshot(new ResearchKey("minecraft:dirt", 0, ""), null, 1);

        ResearchTransaction transaction = new ResearchTransaction(
            42L,
            123456L,
            "D delete",
            Arrays.asList(added),
            Arrays.asList(removed));

        assertEquals(42L, transaction.id());
        assertEquals(123456L, transaction.timestamp());
        assertEquals("D delete", transaction.description());
        assertEquals(added.key(), transaction.added().get(0).key());
        assertEquals(removed.key(), transaction.removed().get(0).key());
        assertFalse(transaction.isEmpty());
    }

    @Test
    public void emptyDeltaIsRecognized() {
        ResearchTransaction transaction = new ResearchTransaction(
            1L,
            1L,
            "noop",
            Collections.<ResearchEntrySnapshot>emptyList(),
            Collections.<ResearchEntrySnapshot>emptyList());
        assertEquals(0, transaction.added().size());
        assertEquals(0, transaction.removed().size());
        org.junit.jupiter.api.Assertions.assertTrue(transaction.isEmpty());
    }
}
