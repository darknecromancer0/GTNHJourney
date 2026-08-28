package dev.gtnhjourney.safety;

/** Aggregates explosion diagnostics while never throttling the cancellation itself. */
public final class ExplosionNotificationThrottle {

    private final long windowMillis;
    private boolean emitted;
    private long lastNotificationMillis;
    private int suppressed;

    public ExplosionNotificationThrottle(long windowMillis) {
        if (windowMillis < 0L) throw new IllegalArgumentException("windowMillis");
        this.windowMillis = windowMillis;
    }

    public synchronized Decision record(long nowMillis) {
        if (!emitted || nowMillis < lastNotificationMillis || nowMillis - lastNotificationMillis >= windowMillis) {
            int priorSuppressed = suppressed;
            emitted = true;
            lastNotificationMillis = nowMillis;
            suppressed = 0;
            return new Decision(true, priorSuppressed);
        }
        suppressed++;
        return new Decision(false, 0);
    }

    public synchronized void reset() {
        emitted = false;
        lastNotificationMillis = 0L;
        suppressed = 0;
    }

    public static final class Decision {

        private final boolean notify;
        private final int suppressedBeforeThis;

        private Decision(boolean notify, int suppressedBeforeThis) {
            this.notify = notify;
            this.suppressedBeforeThis = suppressedBeforeThis;
        }

        public boolean shouldNotify() {
            return notify;
        }

        public int suppressedBeforeThis() {
            return suppressedBeforeThis;
        }
    }
}
