package dev.gtnhjourney.time;

/** Applies one session speed policy: machine-only extra ticks or complete server-world acceleration. */
public final class JourneySpeedController {

    private final JourneySpeedState state;
    private final ServerTickRateAdapter adapter;

    public JourneySpeedController(JourneySpeedState state, ServerTickRateAdapter adapter) {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        if (adapter == null) throw new IllegalArgumentException("adapter must not be null");
        this.state = state;
        this.adapter = adapter;
    }

    public synchronized Result set(JourneySpeedMode mode, int multiplier) {
        if (mode == null || !JourneySpeedState.isAllowedMultiplier(multiplier)) {
            return result(Status.INVALID);
        }

        if (mode == JourneySpeedMode.MACHINES) {
            // Machine-only acceleration must never change the MinecraftServer cadence.
            adapter.reset();
            if (!state.trySet(mode, multiplier)) {
                state.reset();
                return result(Status.INVALID);
            }
            return result(Status.APPLIED);
        }

        if (multiplier == 1) {
            adapter.reset();
            state.trySet(JourneySpeedMode.WORLD, 1);
            return result(Status.APPLIED);
        }

        if (!adapter.isSupported()) {
            failClosed();
            return result(Status.UNSUPPORTED);
        }
        if (!adapter.applyMultiplier(multiplier)) {
            failClosed();
            return result(Status.FAILED);
        }
        if (!state.trySet(JourneySpeedMode.WORLD, multiplier)) {
            failClosed();
            return result(Status.INVALID);
        }
        return result(Status.APPLIED);
    }

    public synchronized Result setMultiplier(int multiplier) {
        return set(state.mode(), multiplier);
    }

    public synchronized JourneySpeedMode mode() {
        return state.mode();
    }

    public synchronized int multiplier() {
        return state.multiplier();
    }

    public synchronized int targetTps() {
        return state.targetTps();
    }

    public synchronized int serverTargetTps() {
        return state.serverTargetTps();
    }

    public synchronized boolean supported() {
        return adapter.isSupported();
    }

    public synchronized void reset() {
        failClosed();
    }

    private void failClosed() {
        adapter.reset();
        state.reset();
    }

    private Result result(Status status) {
        return new Result(status, state.mode(), state.multiplier(), state.targetTps(), state.serverTargetTps());
    }

    public enum Status {
        APPLIED,
        UNSUPPORTED,
        FAILED,
        INVALID
    }

    public static final class Result {

        private final Status status;
        private final JourneySpeedMode mode;
        private final int multiplier;
        private final int targetTps;
        private final int serverTargetTps;

        Result(Status status, JourneySpeedMode mode, int multiplier, int targetTps, int serverTargetTps) {
            this.status = status;
            this.mode = mode;
            this.multiplier = multiplier;
            this.targetTps = targetTps;
            this.serverTargetTps = serverTargetTps;
        }

        public Status status() {
            return status;
        }

        public JourneySpeedMode mode() {
            return mode;
        }

        public int multiplier() {
            return multiplier;
        }

        public int targetTps() {
            return targetTps;
        }

        public int serverTargetTps() {
            return serverTargetTps;
        }
    }
}
