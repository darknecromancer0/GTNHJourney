package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

class JourneyIssuedSortTest {

    @Test
    void issuedOrderIgnoresARecentlyUnlockedButNeverIssuedItem() {
        JourneySortEntry newlyLearned = entry("learned", 0, "a", 100L, 100L, -1L, 0);
        JourneySortEntry actuallyIssued = entry("issued", 1, "b", 10L, 10L, 50L, 1);

        List<JourneySortEntry> sorted = JourneySortPlanner.sort(
            Arrays.asList(newlyLearned, actuallyIssued),
            JourneyGroupMode.NONE,
            JourneyOrderMode.ISSUED,
            false);

        assertEquals("test:issued", sorted.get(0).key().getItemId());
        assertEquals("test:learned", sorted.get(1).key().getItemId());
    }

    @Test
    void issuedOrderRanksNativeFamiliesByNewestIssueButKeepsNativeSubtypeOrderInsideFamily() {
        JourneySortEntry familyOld = entry("family_old", 1, "family", 1L, 1L, 10L, 0);
        JourneySortEntry familyNewest = entry("family_new", 2, "family", 2L, 2L, 100L, 1);
        JourneySortEntry other = entry("other", 3, "other", 3L, 3L, 50L, 2);

        List<JourneySortEntry> sorted = JourneySortPlanner.sort(
            Arrays.asList(other, familyOld, familyNewest),
            JourneyGroupMode.NATIVE,
            JourneyOrderMode.ISSUED,
            false);

        assertEquals("test:family_old", sorted.get(0).key().getItemId());
        assertEquals("test:family_new", sorted.get(1).key().getItemId());
        assertEquals("test:other", sorted.get(2).key().getItemId());
    }

    @Test
    void legacyLatestRemainsPrimaryWhenUserExplicitlyCombinesLatestAndIssued() {
        JourneySortEntry latestActivity = entry("latest", 0, "a", 1L, 100L, 1L, 0);
        JourneySortEntry latestIssue = entry("issued", 1, "b", 1L, 10L, 100L, 1);

        List<JourneySortEntry> sorted = JourneySortPlanner.sort(
            Arrays.asList(latestIssue, latestActivity),
            JourneyGroupMode.NONE,
            JourneyOrderMode.ISSUED,
            true);

        assertEquals("test:latest", sorted.get(0).key().getItemId());
        assertEquals("test:issued", sorted.get(1).key().getItemId());
    }

    private static JourneySortEntry entry(String name, int nativeIndex, String family, long unlock, long activity,
        long issued, int canonical) {
        ResearchKey key = new ResearchKey("test:" + name, 0, "");
        return new JourneySortEntry(
            key, null, nativeIndex, family, "test", "item", "item", name,
            unlock, activity, issued, -1L, canonical);
    }
}
