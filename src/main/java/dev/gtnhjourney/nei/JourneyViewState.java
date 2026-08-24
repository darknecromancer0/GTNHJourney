package dev.gtnhjourney.nei;

/** Client-side mode for the NEI item panel. */
public final class JourneyViewState {

    public enum Mode {
        ALL,
        RESEARCHED,
        NEWEST
    }

    private static volatile Mode mode = Mode.ALL;
    private static volatile long revision;

    private JourneyViewState() {}

    public static Mode mode() {
        return mode;
    }

    public static boolean isEnabled() {
        return mode != Mode.ALL;
    }

    public static boolean isNewest() {
        return mode == Mode.NEWEST;
    }

    public static synchronized boolean toggle() {
        setMode(mode == Mode.RESEARCHED ? Mode.ALL : Mode.RESEARCHED);
        return mode == Mode.RESEARCHED;
    }

    public static synchronized boolean toggleNewest() {
        setMode(mode == Mode.NEWEST ? Mode.ALL : Mode.NEWEST);
        return mode == Mode.NEWEST;
    }

    public static synchronized void setEnabled(boolean value) {
        setMode(value ? Mode.RESEARCHED : Mode.ALL);
    }

    public static synchronized void setMode(Mode value) {
        Mode next = value == null ? Mode.ALL : value;
        if (mode == next) return;
        mode = next;
        revision++;
    }

    public static long revision() {
        return revision;
    }
}
