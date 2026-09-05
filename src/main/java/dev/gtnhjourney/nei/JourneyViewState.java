package dev.gtnhjourney.nei;

/** Client-side content mode for the NEI item panel. Sorting is owned separately by JourneySortState. */
public final class JourneyViewState {
    public enum Mode { ALL, RESEARCHED, FAVOURITE, CREATIVE, DELETE }

    private static volatile Mode mode = Mode.ALL;
    private static volatile long revision;

    private JourneyViewState() {}

    public static Mode mode() { return mode; }
    public static boolean isEnabled() { return mode != Mode.ALL; }
    public static boolean isFavourite() { return mode == Mode.FAVOURITE; }
    public static boolean isCreative() { return mode == Mode.CREATIVE; }
    public static boolean isDelete() { return mode == Mode.DELETE; }

    /** Compatibility bridge for the removed N view: old Newest semantics are now J + L. */
    @Deprecated
    public static boolean isNewest() {
        return mode == Mode.RESEARCHED && JourneySortState.latest(Mode.RESEARCHED)
            && JourneySortState.group(Mode.RESEARCHED) == JourneyGroupMode.NONE
            && JourneySortState.order(Mode.RESEARCHED) == JourneyOrderMode.NONE;
    }

    public static synchronized boolean toggle() {
        setMode(mode == Mode.RESEARCHED ? Mode.ALL : Mode.RESEARCHED);
        return mode == Mode.RESEARCHED;
    }

    /** Compatibility bridge for callers compiled around the old N control. */
    @Deprecated
    public static synchronized boolean toggleNewest() {
        setMode(Mode.RESEARCHED);
        JourneySortState.setGroup(Mode.RESEARCHED, JourneyGroupMode.NONE);
        JourneySortState.setOrder(Mode.RESEARCHED, JourneyOrderMode.NONE);
        JourneySortState.toggleLatest(Mode.RESEARCHED);
        return JourneySortState.latest(Mode.RESEARCHED);
    }

    public static synchronized boolean toggleFavourite() {
        setMode(mode == Mode.FAVOURITE ? Mode.ALL : Mode.FAVOURITE);
        return mode == Mode.FAVOURITE;
    }

    public static synchronized boolean toggleCreative() {
        setMode(mode == Mode.CREATIVE ? Mode.ALL : Mode.CREATIVE);
        return mode == Mode.CREATIVE;
    }

    public static synchronized boolean toggleDelete() {
        setMode(mode == Mode.DELETE ? Mode.ALL : Mode.DELETE);
        return mode == Mode.DELETE;
    }

    public static synchronized void setEnabled(boolean value) { setMode(value ? Mode.RESEARCHED : Mode.ALL); }

    public static synchronized void setMode(Mode value) {
        Mode next = value == null ? Mode.ALL : value;
        if (mode == next) return;
        mode = next;
        revision++;
    }

    public static long revision() { return revision; }
}
