package dev.gtnhjourney.nei;

/** Tiny six-pixel gold star, deliberately separate from item stacks and their quantity overlays. */
public final class JourneyFavouriteStarShape {

    public static final int SIZE = 6;
    // Bit zero is the leftmost pixel. Distinct outline/silhouette at normal and scaled NEI slot sizes.
    private static final int[] ROWS = { 0x0C, 0x0C, 0x3F, 0x1E, 0x33, 0x21 };

    private JourneyFavouriteStarShape() {}

    public static boolean isPixel(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && ((ROWS[y] >> x) & 1) != 0;
    }

    public static int markerX(int slotX, int slotWidth) {
        return slotX + slotWidth - SIZE - 1;
    }

    public static int markerY(int slotY) {
        return slotY + 1;
    }
}
