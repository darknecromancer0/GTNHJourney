package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import vazkii.botania.api.mana.IManaCollector;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.subtile.SubTileFunctional;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.tile.TileSpecialFlower;

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
    public void manaSpreaderCollectorReadsCurrentAndMaximumMana() {
        BotaniaManaPoolInspector.Result result = BotaniaManaPoolInspector.inspect(new FakeCollector(640, 1000));

        assertEquals(640, result.currentMana());
        assertEquals(1000, result.capacity());
        assertEquals(360, result.freeMana());
        assertEquals(64.0D, result.percent(), 0.0001D);
    }

    @Test
    public void generatingFlowerReadsCurrentManaFromSubtilePacketNbt() {
        TileSpecialFlower flower = new TileSpecialFlower(new SubTileGenerating(17, 75));

        BotaniaManaPoolInspector.Result result = BotaniaManaPoolInspector.inspect(flower);

        assertEquals(17, result.currentMana());
        assertEquals(75, result.capacity());
        assertEquals(58, result.freeMana());
    }

    @Test
    public void functionalFlowerReadsCurrentManaFromSubtilePacketNbt() {
        TileSpecialFlower flower = new TileSpecialFlower(new SubTileFunctional(120, 300));

        BotaniaManaPoolInspector.Result result = BotaniaManaPoolInspector.inspect(flower);

        assertEquals(120, result.currentMana());
        assertEquals(300, result.capacity());
        assertEquals(180, result.freeMana());
    }

    @Test
    public void arbitraryManaLookingObjectIsRejected() {
        assertNull(BotaniaManaPoolInspector.inspect(new FakeManaLookingObject()));
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

    private static final class FakeCollector implements IManaCollector {

        private final int current;
        private final int max;

        FakeCollector(int current, int max) {
            this.current = current;
            this.max = max;
        }

        @Override
        public int getCurrentMana() {
            return current;
        }

        @Override
        public int getMaxMana() {
            return max;
        }
    }

    private static final class FakeManaLookingObject {

        public int getCurrentMana() {
            return 50;
        }

        public int getMaxMana() {
            return 100;
        }
    }
}
