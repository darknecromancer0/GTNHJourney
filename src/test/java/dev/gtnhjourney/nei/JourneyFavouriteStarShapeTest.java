package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JourneyFavouriteStarShapeTest {

    @Test
    void sixPixelStarIsRecognizableAndNotAFullRectangle() {
        assertEquals(6, JourneyFavouriteStarShape.SIZE);
        assertTrue(JourneyFavouriteStarShape.isPixel(2, 0));
        assertTrue(JourneyFavouriteStarShape.isPixel(0, 2));
        assertTrue(JourneyFavouriteStarShape.isPixel(5, 2));
        assertTrue(JourneyFavouriteStarShape.isPixel(0, 5));
        assertTrue(JourneyFavouriteStarShape.isPixel(5, 5));
        assertFalse(JourneyFavouriteStarShape.isPixel(0, 0));
        assertFalse(JourneyFavouriteStarShape.isPixel(6, 0));
        assertFalse(JourneyFavouriteStarShape.isPixel(2, -1));
    }

    @Test
    void markerUsesTopRightCornerAndAvoidsBottomRightItemQuantity() {
        assertEquals(11, JourneyFavouriteStarShape.markerX(0, 18));
        assertEquals(1, JourneyFavouriteStarShape.markerY(0));
        assertEquals(32, JourneyFavouriteStarShape.markerX(20, 19));
    }
}
