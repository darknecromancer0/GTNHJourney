package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class JourneyListSanitizerTest {

    @Test
    public void removesOnlyMatchingEntriesAndPreservesRelativeOrder() {
        List<String> values = new ArrayList<String>(Arrays.asList("safe-a", "bad-a", "safe-b", "bad-b", "safe-c"));

        int removed = JourneyListSanitizer.removeMatching(values, new JourneyListSanitizer.Matcher<String>() {

            @Override
            public boolean matches(String value) {
                return value != null && value.startsWith("bad-");
            }
        });

        assertEquals(2, removed);
        assertEquals(Arrays.asList("safe-a", "safe-b", "safe-c"), values);
    }

    @Test
    public void nullListOrMatcherIsANoOp() {
        assertEquals(0, JourneyListSanitizer.removeMatching(null, new JourneyListSanitizer.Matcher<String>() {

            @Override
            public boolean matches(String value) {
                return true;
            }
        }));
        List<String> values = new ArrayList<String>(Arrays.asList("safe"));
        assertEquals(0, JourneyListSanitizer.removeMatching(values, null));
        assertEquals(Arrays.asList("safe"), values);
    }
}
