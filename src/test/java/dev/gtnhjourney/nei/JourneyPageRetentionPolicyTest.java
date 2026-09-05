package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JourneyPageRetentionPolicyTest {

    @Test
    void activityOnlyRefreshKeepsPageTwoAndBeyond() {
        assertEquals(1, JourneyPageRetentionPolicy.pageAfterRefresh(1, 8, false));
        assertEquals(5, JourneyPageRetentionPolicy.pageAfterRefresh(5, 8, false));
    }

    @Test
    void retainedPageClampsOnlyWhenResultSetActuallyShrinks() {
        assertEquals(2, JourneyPageRetentionPolicy.pageAfterRefresh(7, 3, false));
        assertEquals(0, JourneyPageRetentionPolicy.pageAfterRefresh(7, 0, false));
    }

    @Test
    void explicitViewFilterOrSortChangeMayResetToFirstPage() {
        assertEquals(0, JourneyPageRetentionPolicy.pageAfterRefresh(5, 8, true));
    }
}
