package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JourneySpeedControllerTest {

    @Test
    public void machinesModeKeepsWorldCadenceAtOne() {
        FakeAdapter adapter = new FakeAdapter(true, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.set(JourneySpeedMode.MACHINES, 16);

        assertEquals(JourneySpeedController.Status.APPLIED, result.status());
        assertEquals(JourneySpeedMode.MACHINES, controller.mode());
        assertEquals(16, controller.multiplier());
        assertEquals(20, controller.serverTargetTps());
        assertEquals(1, adapter.lastApplied);
        assertEquals(0, adapter.applyCalls);
    }

    @Test
    public void worldModeUsesServerCadenceAdapter() {
        FakeAdapter adapter = new FakeAdapter(true, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.set(JourneySpeedMode.WORLD, 64);

        assertEquals(JourneySpeedController.Status.APPLIED, result.status());
        assertEquals(JourneySpeedMode.WORLD, result.mode());
        assertEquals(64, result.multiplier());
        assertEquals(1280, result.serverTargetTps());
        assertEquals(64, adapter.lastApplied);
    }

    @Test
    public void unsupportedWorldModeFailsClosedToMachinesOne() {
        FakeAdapter adapter = new FakeAdapter(false, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.set(JourneySpeedMode.WORLD, 8);

        assertEquals(JourneySpeedController.Status.UNSUPPORTED, result.status());
        assertEquals(JourneySpeedMode.MACHINES, controller.mode());
        assertEquals(1, controller.multiplier());
        assertEquals(1, adapter.lastApplied);
    }

    @Test
    public void failedWorldApplicationRestoresSafeDefault() {
        FakeAdapter adapter = new FakeAdapter(true, false);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.set(JourneySpeedMode.WORLD, 2);

        assertEquals(JourneySpeedController.Status.FAILED, result.status());
        assertEquals(JourneySpeedMode.MACHINES, controller.mode());
        assertEquals(1, controller.multiplier());
        assertEquals(1, adapter.lastApplied);
    }

    @Test
    public void invalidMultiplierDoesNotApplyWorldAdapter() {
        FakeAdapter adapter = new FakeAdapter(true, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);

        JourneySpeedController.Result result = controller.set(JourneySpeedMode.WORLD, 3);

        assertEquals(JourneySpeedController.Status.INVALID, result.status());
        assertEquals(1, controller.multiplier());
        assertEquals(0, adapter.applyCalls);
    }

    @Test
    public void resetRestoresMachinesOne() {
        FakeAdapter adapter = new FakeAdapter(true, true);
        JourneySpeedController controller = new JourneySpeedController(new JourneySpeedState(), adapter);
        controller.set(JourneySpeedMode.WORLD, 8);

        controller.reset();

        assertEquals(JourneySpeedMode.MACHINES, controller.mode());
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
