package dev.gtnhjourney.nei;

/** Pure presentation rules for the small Journey controls embedded in NEI's item-panel header. */
public final class JourneyButtonPresentation {
    private static final int RESEARCH_MIN_WIDTH = 96;
    // At 114px the second 16px button overlaps the centered "page/total" label. Keep a small safety gap.
    private static final int NEWEST_MIN_WIDTH = 132;

    private JourneyButtonPresentation() {}

    public static boolean researchVisible(int panelWidth) {
        return panelWidth >= RESEARCH_MIN_WIDTH;
    }

    public static boolean newestVisible(int panelWidth) {
        return panelWidth >= NEWEST_MIN_WIDTH;
    }

    public static String researchTooltip(JourneyViewState.Mode mode, int serverOnlyCount) {
        JourneyViewState.Mode effective = mode == null ? JourneyViewState.Mode.ALL : mode;
        final String base;
        switch (effective) {
            case RESEARCHED:
                base = "Journey view: researched only. Click to show all NEI items.";
                break;
            case NEWEST:
                base = "Newest view is active. Click J to switch to researched items.";
                break;
            case ALL:
            default:
                base = "Journey view: show only researched items.";
                break;
        }
        if (serverOnlyCount <= 0) return base;
        return base + " " + serverOnlyCount
            + " state(s) are server-only because their payload could not be safely synchronized.";
    }
}
