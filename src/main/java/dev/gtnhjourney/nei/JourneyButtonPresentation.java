package dev.gtnhjourney.nei;

/** Pure presentation rules for the small Journey controls embedded in NEI's item-panel header. */
public final class JourneyButtonPresentation {
    private static final int RESEARCH_MIN_WIDTH = 96;
    private static final int FAVOURITE_MIN_WIDTH = 132;
    private static final int CREATIVE_MIN_WIDTH = 150;
    private static final int DELETE_MIN_WIDTH = 168;
    private static final int SCAN_MIN_WIDTH = 186;
    private static final int DEBUG_TOOL_MIN_WIDTH = 204;

    private JourneyButtonPresentation() {}

    public static boolean researchVisible(int panelWidth) { return panelWidth >= RESEARCH_MIN_WIDTH; }
    public static boolean favouriteVisible(int panelWidth) { return panelWidth >= FAVOURITE_MIN_WIDTH; }
    public static boolean creativeVisible(int panelWidth) { return panelWidth >= CREATIVE_MIN_WIDTH; }
    public static boolean deleteVisible(int panelWidth) { return panelWidth >= DELETE_MIN_WIDTH; }
    public static boolean scanVisible(int panelWidth) { return panelWidth >= SCAN_MIN_WIDTH; }
    public static boolean debugToolVisible(int panelWidth) { return panelWidth >= DEBUG_TOOL_MIN_WIDTH; }

    /** Legacy copy retained for older contract tests; N is no longer a button in 1.1.26. */
    @Deprecated
    public static String newestTooltip(boolean active) {
        return active
            ? "Latest activity is active for researched Journey items."
            : "Latest activity is now the independent L sorting modifier.";
    }

    public static String scanTooltip() {
        return "Scan inventory: deep-scan player inventory and embedded container contents, then refresh Journey.";
    }

    public static String debugToolTooltip() {
        return "Give Debug Researcher Tool (integrated-server owner/operator only).";
    }

    public static String favouriteTooltip(boolean active) {
        return active
            ? "Favourite view: exact starred Journey states. Alt+RMB removes from F."
            : "Favourite view: show starred researched states. Add with Alt+LMB from J; remove with Alt+RMB in F.";
    }

    public static String creativeTooltip(boolean active) {
        return active
            ? "Creative debug view: native NEI items plus Journey-only exact variants. LMB gives one, RMB gives a stack; Shift+RMB fills empty main slots."
            : "Creative debug view: native NEI universe plus Journey exact variants missing from ordinary NEI.";
    }

    public static String researchTooltip(JourneyViewState.Mode mode, int serverOnlyCount) {
        JourneyViewState.Mode effective = mode == null ? JourneyViewState.Mode.ALL : mode;
        final String base;
        switch (effective) {
            case RESEARCHED:
                base = "Journey view: researched only. Alt+LMB adds to F; LMB gives one; RMB gives a stack.";
                break;
            case FAVOURITE:
                base = "Favourite view is active. Alt+RMB removes from F. Click J to switch to researched items.";
                break;
            case CREATIVE:
                base = "Creative debug view is active. Click J to switch to researched items.";
                break;
            case DELETE:
                base = "Delete view is active. Click J to switch to researched items.";
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
