package dev.gtnhjourney.nei;

/** Pure click semantics shared by the NEI input adapter and tests. */
public final class JourneyRetrieveClickPolicy {
    private JourneyRetrieveClickPolicy() {}

    /**
     * Journey views own LMB/RMB on researched panel entries. Ordinary NEI requires Ctrl so normal recipe/usage clicks
     * remain untouched.
     */
    public static boolean shouldRetrieve(int button, boolean journeyView, boolean controlDown) {
        return (button == 0 || button == 1) && (journeyView || controlDown);
    }

    /** RMB or Shift requests one; plain LMB requests one natural max stack. */
    public static int requestedAmount(int button, boolean shiftDown, int maxStackSize) {
        if (button == 1 || shiftDown) return 1;
        return Math.max(1, maxStackSize);
    }
}
