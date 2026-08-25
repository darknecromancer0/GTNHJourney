package dev.gtnhjourney.acquisition;

import java.util.Objects;

/** Immutable dimension + block-position identity for a tracked furnace. */
public final class FurnaceKey {

    private final int dimension;
    private final int x;
    private final int y;
    private final int z;

    public FurnaceKey(int dimension, int x, int y, int z) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int dimension() {
        return dimension;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FurnaceKey)) return false;
        FurnaceKey that = (FurnaceKey) other;
        return dimension == that.dimension && x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(dimension), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z));
    }
}
