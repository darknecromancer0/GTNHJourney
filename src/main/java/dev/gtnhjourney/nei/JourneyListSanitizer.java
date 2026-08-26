package dev.gtnhjourney.nei;

import java.util.Iterator;
import java.util.List;

/** Small in-place list primitive used by client-only Creative safety without rebuilding unrelated entries. */
final class JourneyListSanitizer {

    interface Matcher<T> {
        boolean matches(T value);
    }

    private JourneyListSanitizer() {}

    static <T> int removeMatching(List<T> values, Matcher<T> matcher) {
        if (values == null || matcher == null || values.isEmpty()) return 0;
        int removed = 0;
        Iterator<T> iterator = values.iterator();
        while (iterator.hasNext()) {
            if (!matcher.matches(iterator.next())) continue;
            iterator.remove();
            removed++;
        }
        return removed;
    }
}
