package dev.gtnhjourney.nei;

/** Pure safety policy for clicks while the Journey D panel mode is active. */
public final class JourneyDeleteClickPolicy {

    private JourneyDeleteClickPolicy() {}

    /** pre7 deliberately supports only an unmodified left click for exact single-state deletion. */
    public static boolean shouldDelete(int button, boolean shiftDown) {
        return button == 0 && !shiftDown;
    }
}
