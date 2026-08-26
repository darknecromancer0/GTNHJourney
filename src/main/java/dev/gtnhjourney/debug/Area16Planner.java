package dev.gtnhjourney.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure fixed-radius planner for the migration AREA_16 scan. */
public final class Area16Planner {

    private static final int RADIUS = 16;
    private static final int DIAMETER = RADIUS * 2 + 1;

    private Area16Planner() {}

    public static List<Position> plan(int centerX, int centerY, int centerZ) {
        List<Position> positions = new ArrayList<Position>(DIAMETER * DIAMETER * DIAMETER);
        for (int x = centerX - RADIUS; x <= centerX + RADIUS; x++) {
            for (int y = centerY - RADIUS; y <= centerY + RADIUS; y++) {
                for (int z = centerZ - RADIUS; z <= centerZ + RADIUS; z++) {
                    positions.add(new Position(x, y, z));
                }
            }
        }
        return Collections.unmodifiableList(positions);
    }

    public static final class Position {

        private final int x;
        private final int y;
        private final int z;

        public Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Position)) return false;
            Position position = (Position) other;
            return x == position.x && y == position.y && z == position.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }

        @Override
        public String toString() {
            return "Position{" + x + ',' + y + ',' + z + '}';
        }
    }
}
