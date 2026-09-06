package dev.gtnhjourney.nei;

/** Pure viewport geometry for Journey dropdown overlays. */
final class JourneyDropdownViewport {

    private JourneyDropdownViewport() {}

    static int clampPopupX(int anchorX, int popupWidth, int screenWidth) {
        int width = Math.max(0, popupWidth);
        int maxX = Math.max(0, screenWidth - width);
        return Math.max(0, Math.min(anchorX, maxX));
    }
}
