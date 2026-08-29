package dev.gtnhjourney.client;

import java.util.concurrent.atomic.AtomicLong;

/** Read-only runtime state for diagnosing the live /journey chat suggestion overlay. */
public final class CommandHintDiagnostics {

    private static final AtomicLong RESOLVER_FAILURES = new AtomicLong();
    private static volatile boolean registered;
    private static volatile String resolverPath = "UNRESOLVED";
    private static volatile int lastSuggestionCount;

    private CommandHintDiagnostics() {}

    public static void markRegistered() {
        registered = true;
    }

    public static void recordResolverSuccess(String path) {
        if (path != null && !path.isEmpty()) resolverPath = path;
    }

    public static void recordResolverFailure() {
        RESOLVER_FAILURES.incrementAndGet();
    }

    public static void recordSuggestionCount(int count) {
        lastSuggestionCount = Math.max(0, count);
    }

    public static Snapshot snapshot() {
        return new Snapshot(registered, resolverPath, RESOLVER_FAILURES.get(), lastSuggestionCount);
    }

    static void resetForTests() {
        registered = false;
        resolverPath = "UNRESOLVED";
        RESOLVER_FAILURES.set(0L);
        lastSuggestionCount = 0;
    }

    public static final class Snapshot {

        private final boolean registered;
        private final String resolverPath;
        private final long resolverFailures;
        private final int lastSuggestionCount;

        Snapshot(boolean registered, String resolverPath, long resolverFailures, int lastSuggestionCount) {
            this.registered = registered;
            this.resolverPath = resolverPath;
            this.resolverFailures = resolverFailures;
            this.lastSuggestionCount = lastSuggestionCount;
        }

        public boolean registered() {
            return registered;
        }

        public String resolverPath() {
            return resolverPath;
        }

        public long resolverFailures() {
            return resolverFailures;
        }

        public int lastSuggestionCount() {
            return lastSuggestionCount;
        }
    }
}
