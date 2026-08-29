package dev.gtnhjourney.time;

/** Applies session speed changes only when the transformed server-loop hook is proven available. */
public final class JourneySpeedController {

    private final JourneySpeedState state;
    private final ServerTickRateAdapter adapter;

    public JourneySpeedController(JourneySpeedState state, ServerTickRateAdapter adapter) {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        if (adapter == null) throw new IllegalArgumentException("adapter must not be null");
        this.state = state;
        this.adapter = adapter;
    }

    public synchronized Result setMultiplier(int multiplier) {
        if (!JourneySpeedState.isAllowedMultiplier(multiplier)) {
            return new Result(Status.INVALID, state.multiplier(), state.targetTps());
        }

        if (multiplier == 1) {
            adapter.reset();
            state.reset();
            return new Result(Status.APPLIED, 1, 20);
        }

        if (!adapter.isSupported()) {
            adapter.reset();
            state.reset();
            return new Result(Status.UNSUPPORTED, 1, 20);
        }

        if (!adapter.applyMultiplier(multiplier)) {
            adapter.reset();
            state.reset();
            return new Result(Status.FAILED, 1, 20);
        }

        if (!state.trySetMultiplier(multiplier)) {
            adapter.reset();
            state.reset();
            return new Result(Status.INVALID, 1, 20);
        }
        return new Result(Status.APPLIED, state.multiplier(), state.targetTps());
    }

    public synchronized int multiplier() {
        return state.multiplier();
    }

    public synchronized int targetTps() {
        return state.targetTps();
    }

    public synchronized boolean supported() {
        return adapter.isSupported();
    }

    public synchronized void reset() {
        adapter.reset();
        state.reset();
    }

    public enum Status {
        APPLIED,
        UNSUPPORTED,
        FAILED,
        INVALID
    }

    public static final class Result {

        private final Status status;
        private final int multiplier;
        private final int targetTps;

        Result(Status status, int multiplier, int targetTps) {
            this.status = status;
            this.multiplier = multiplier;
            this.targetTps = targetTps;
        }

        public Status status() {
            return status;
        }

        public int multiplier() {
            return multiplier;
        }

        public int targetTps() {
            return targetTps;
        }
    }
}
