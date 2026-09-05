package dev.gtnhjourney.nei;

/** Pure click semantics shared by the NEI input adapter and tests. */
public final class JourneyRetrieveClickPolicy {

    private JourneyRetrieveClickPolicy() {}

    /** Journey-owned views issue only on RMB. Legacy Ctrl+click outside Journey remains compatible. */
    public static boolean shouldRetrieve(int button, boolean journeyView, boolean controlDown) {
        if (journeyView) return button == 1;
        return (button == 0 || button == 1) && controlDown;
    }

    public static boolean shouldToggleFavourite(JourneyViewState.Mode mode, int button, boolean altDown) {
        if (button != 0 || !altDown || mode == null) return false;
        return mode == JourneyViewState.Mode.RESEARCHED || mode == JourneyViewState.Mode.NEWEST
            || mode == JourneyViewState.Mode.FAVOURITE;
    }

    /** Plain LMB is consumed in Journey-owned views so it cannot fall through to one-item NEI issuance. */
    public static boolean shouldConsumePlainLeftClick(JourneyViewState.Mode mode, int button) {
        return button == 0 && mode != null && mode != JourneyViewState.Mode.ALL && mode != JourneyViewState.Mode.DELETE;
    }

    public static boolean shouldFillInventory(int button, boolean shiftDown) { return button == 1 && shiftDown; }

    public static int requestedAmount(int button, boolean shiftDown, int maxStackSize) {
        if (button == 1) return Math.max(1, maxStackSize);
        return 1;
    }
}
