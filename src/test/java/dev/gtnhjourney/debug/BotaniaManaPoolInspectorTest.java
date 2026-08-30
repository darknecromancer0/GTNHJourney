package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import vazkii.botania.api.mana.IManaPool;

public class BotaniaManaPoolInspectorTest {

    @Test
    public void readsExactManaCapacityFreeSpaceAndPercent() {
        BotaniaManaPoolInspector.Result result = BotaniaManaPoolInspector.inspect(new FakePool(250000, 750000));

        assertEquals(250000, result.currentMana());
        assertEquals(1000000, result.capacity());
        assertEquals(750000, result.freeMana());
        assertEquals(25.0D, result.percent(), 0.0001D);
    }

    @Test
    public void dilutedPoolCapacityComesFromRuntimeAvailableSpace() {
        BotaniaManaPoolInspector.Result result = BotaniaManaPoolInspector.inspect(new FakePool(2500, 7500));

        assertEquals(2500, result.currentMana());
        assertEquals(10000, result.capacity());
        assertEquals(25.0D, result.percent(), 0.0001D);
    }

    @Test
    public void unrelatedObjectIsRejected() {
        assertNull(BotaniaManaPoolInspector.inspect(new Object()));
    }

    private static final class FakePool implements IManaPool {

        private final int current;
        private final int free;

        FakePool(int current, int free) {
            this.current = current;
            this.free = free;
        }

        @Override
        public int getCurrentMana() {
            return current;
        }

        @Override
        public int getAvailableSpaceForMana() {
            return free;
        }
    }
}
