package dev.gtnhjourney.nei;

/** Pure click semantics shared by the NEI input adapter and tests. */
public final class JourneyRetrieveClickPolicy {

    private JourneyRetrieveClickPolicy() {}

    /** Journey-owned issue views use the same LMB/RMB semantics; legacy Ctrl+click outside Journey stays compatible. */
    public static boolean shouldRetrieve(int button, boolean journeyView, boolean controlDown) {
        if (journeyView) return button == 0 || button == 1;
        return (button == 0 || button == 1) && controlDown;
    }

    /** Alt+LMB is the one deliberate exception: it toggles F instead of issuing an item on that click. */
    public static boolean shouldToggleFavourite(JourneyViewState.Mode mode, int button, boolean altDown) {
        if (button != 0 || !altDown || mode == null) return false;
        return mode == JourneyViewState.Mode.RESEARCHED || mode == JourneyViewState.Mode.NEWEST
            || mode == JourneyViewState.Mode.FAVOURITE;
    }

    public static boolean shouldFillInventory(int button, boolean shiftDown) { return button == 1 && shiftDown; }

    /** LMB requests one item. RMB requests one natural max stack. */
    public static int requestedAmount(int button, boolean shiftDown, int maxStackSize) {
        if (button == 1) return Math.max(1, maxStackSize);
        return 1;
    }
}
