package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class FurnaceKeyTest {

    @Test
    public void equalityIncludesDimensionAndCoordinates() {
        FurnaceKey base = new FurnaceKey(0, 10, 64, -5);

        assertEquals(base, new FurnaceKey(0, 10, 64, -5));
        assertEquals(base.hashCode(), new FurnaceKey(0, 10, 64, -5).hashCode());
        assertNotEquals(base, new FurnaceKey(1, 10, 64, -5));
        assertNotEquals(base, new FurnaceKey(0, 11, 64, -5));
        assertNotEquals(base, new FurnaceKey(0, 10, 65, -5));
        assertNotEquals(base, new FurnaceKey(0, 10, 64, -4));
    }
}
