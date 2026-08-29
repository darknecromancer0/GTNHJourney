package dev.gtnhjourney.nei;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only snapshot of the NEI filters Journey actually applies to its panel. */
public final class JourneyFilterDiagnostics {

    private static volatile Snapshot snapshot = new Snapshot(Collections.<String>emptyList(), "UNAVAILABLE");

    private JourneyFilterDiagnostics() {}

    public static void record(List<String> providerClassNames, String searchText) {
        List<String> providers = providerClassNames == null ? Collections.<String>emptyList() : providerClassNames;
        snapshot = new Snapshot(providers, normalizeSearchText(searchText));
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    static String safeSearchText(Object searchField) {
        if (searchField == null) return "UNAVAILABLE";
        String[] methodNames = { "getText", "text", "getSearchText" };
        for (String methodName : methodNames) {
            try {
                Method method = findNoArgMethod(searchField.getClass(), methodName);
                if (method == null) continue;
                if (!method.isAccessible()) method.setAccessible(true);
                Object value = method.invoke(searchField);
                return value == null ? "" : String.valueOf(value);
            } catch (ReflectiveOperationException ignored) {
                return "UNAVAILABLE";
            } catch (RuntimeException ignored) {
                return "UNAVAILABLE";
            } catch (LinkageError ignored) {
                return "UNAVAILABLE";
            }
        }
        return "UNAVAILABLE";
    }

    static void resetForTests() {
        snapshot = new Snapshot(Collections.<String>emptyList(), "UNAVAILABLE");
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private static String normalizeSearchText(String value) {
        return value == null ? "UNAVAILABLE" : value;
    }

    public static final class Snapshot {

        private final List<String> providerClassNames;
        private final String searchText;

        Snapshot(List<String> providerClassNames, String searchText) {
            this.providerClassNames = Collections.unmodifiableList(new ArrayList<String>(providerClassNames));
            this.searchText = normalizeSearchText(searchText);
        }

        public int providerCount() {
            return providerClassNames.size();
        }

        public List<String> providerClassNames() {
            return providerClassNames;
        }

        public String searchText() {
            return searchText;
        }
    }
}
