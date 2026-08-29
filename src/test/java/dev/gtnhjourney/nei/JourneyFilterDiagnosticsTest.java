package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class JourneyFilterDiagnosticsTest {

    @AfterEach
    public void reset() {
        JourneyFilterDiagnostics.resetForTests();
    }

    @Test
    public void snapshotKeepsAppliedProviderNamesAndSearchText() {
        JourneyFilterDiagnostics.record(Arrays.asList("example.SearchProvider", "example.OtherProvider"), "lithium");

        JourneyFilterDiagnostics.Snapshot snapshot = JourneyFilterDiagnostics.snapshot();
        assertEquals(2, snapshot.providerCount());
        assertEquals(Arrays.asList("example.SearchProvider", "example.OtherProvider"), snapshot.providerClassNames());
        assertEquals("lithium", snapshot.searchText());
    }

    @Test
    public void searchTextReflectionFailsClosedToMarker() {
        assertEquals("battery", JourneyFilterDiagnostics.safeSearchText(new SearchFixture()));
        assertEquals("UNAVAILABLE", JourneyFilterDiagnostics.safeSearchText(new BrokenSearchFixture()));
        assertEquals("UNAVAILABLE", JourneyFilterDiagnostics.safeSearchText(null));
    }

    public static final class SearchFixture {
        public String getText() {
            return "battery";
        }
    }

    public static final class BrokenSearchFixture {
        public String getText() {
            throw new IllegalStateException("broken");
        }
    }
}
