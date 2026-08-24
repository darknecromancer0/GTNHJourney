package dev.gtnhjourney.nei;

/** Stable user-facing Journey hints, isolated so they can be tested without Minecraft/NEI. */
public final class JourneyTooltipText {
    private JourneyTooltipText() {}

    public static String researchedLine() {
        return "Journey: Researched";
    }

    public static String fullStackHint(boolean journeyView) {
        return journeyView ? "Journey: LMB = stack" : "Journey: Ctrl+LMB = stack";
    }

    public static String singleItemHint(boolean journeyView) {
        return journeyView ? "Journey: RMB / Shift+LMB = one" : "Journey: Ctrl+RMB / Ctrl+Shift+LMB = one";
    }
}
