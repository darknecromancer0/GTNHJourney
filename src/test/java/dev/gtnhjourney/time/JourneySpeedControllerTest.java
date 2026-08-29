package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JourneySpeedControllerTest {

    @Test
    public void successfulApplicationUpdatesSessionState() {
        FakeAdapter adapter = new FakeAdapter(true, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.setMultiplier(4);

        assertEquals(JourneySpeedController.Status.APPLIED, result.status());
        assertEquals(4, result.multiplier());
        assertEquals(4, controller.multiplier());
        assertEquals(4, adapter.lastApplied);
    }

    @Test
    public void unsupportedAdapterFailsClosedAtOne() {
        FakeAdapter adapter = new FakeAdapter(false, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.setMultiplier(8);

        assertEquals(JourneySpeedController.Status.UNSUPPORTED, result.status());
        assertEquals(1, controller.multiplier());
        assertEquals(1, adapter.lastApplied);
    }

    @Test
    public void failedApplicationRestoresOne() {
        FakeAdapter adapter = new FakeAdapter(true, false);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.setMultiplier(2);

        assertEquals(JourneySpeedController.Status.FAILED, result.status());
        assertEquals(1, controller.multiplier());
        assertEquals(1, adapter.lastApplied);
    }

    @Test
    public void invalidMultiplierDoesNotTouchAdapterOrState() {
        FakeAdapter adapter = new FakeAdapter(true, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.setMultiplier(3);

        assertEquals(JourneySpeedController.Status.INVALID, result.status());
        assertEquals(1, controller.multiplier());
        assertEquals(0, adapter.applyCalls);
    }

    @Test
    public void resetRestoresAdapterAndState() {
        FakeAdapter adapter = new FakeAdapter(true, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);
        controller.setMultiplier(8);

        controller.reset();

        assertEquals(1, controller.multiplier());
        assertEquals(1, adapter.lastApplied);
        assertEquals(1, adapter.resetCalls);
    }

    private static final class FakeAdapter implements ServerTickRateAdapter {
        private final boolean supported;
        private final boolean applies;
        private int lastApplied;
        private int applyCalls;
        private int resetCalls;

        FakeAdapter(boolean supported, boolean applies) {
            this.supported = supported;
            this.applies = applies;
        }

        @Override
        public boolean isSupported() {
            return supported;
        }

        @Override
        public boolean applyMultiplier(int multiplier) {
            applyCalls++;
            lastApplied = multiplier;
            return applies;
        }

        @Override
        public void reset() {
            resetCalls++;
            lastApplied = 1;
        }
    }
}
