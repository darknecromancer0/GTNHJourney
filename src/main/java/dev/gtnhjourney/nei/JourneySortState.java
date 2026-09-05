package dev.gtnhjourney.nei;

import java.util.EnumMap;
import java.util.Map;

/** Session-local Group × Order × Latest state remembered independently for J/F/C/D. */
public final class JourneySortState {

    private static final Map<JourneyViewState.Mode, Settings> values =
        new EnumMap<JourneyViewState.Mode, Settings>(JourneyViewState.Mode.class);
    private static long revision;

    static {
        resetInternal();
    }

    private JourneySortState() {}

    public static synchronized JourneyGroupMode group(JourneyViewState.Mode mode) {
        return settings(mode).group;
    }

    public static synchronized JourneyOrderMode order(JourneyViewState.Mode mode) {
        return settings(mode).order;
    }

    public static synchronized boolean latest(JourneyViewState.Mode mode) {
        return settings(mode).latest;
    }

    public static synchronized void setGroup(JourneyViewState.Mode mode, JourneyGroupMode value) {
        Settings settings = settings(mode);
        JourneyGroupMode next = value == null ? JourneyGroupMode.NONE : value;
        if (settings.group == next) return;
        settings.group = next;
        revision++;
    }

    public static synchronized void setOrder(JourneyViewState.Mode mode, JourneyOrderMode value) {
        Settings settings = settings(mode);
        JourneyOrderMode next = normalizeOrder(mode, value);
        if (settings.order == next) return;
        settings.order = next;
        revision++;
    }

    public static synchronized void setLatest(JourneyViewState.Mode mode, boolean value) {
        Settings settings = settings(mode);
        if (settings.latest == value) return;
        settings.latest = value;
        revision++;
    }

    public static synchronized void toggleLatest(JourneyViewState.Mode mode) {
        setLatest(mode, !latest(mode));
    }

    public static synchronized long revision() { return revision; }

    public static synchronized void reset() {
        resetInternal();
        revision++;
    }

    private static JourneyOrderMode normalizeOrder(JourneyViewState.Mode mode, JourneyOrderMode value) {
        JourneyOrderMode next = value == null ? JourneyOrderMode.NONE : value;
        if (next == JourneyOrderMode.FAVOURITE_ADDED && mode != JourneyViewState.Mode.FAVOURITE) {
            return JourneyOrderMode.NONE;
        }
        return next;
    }

    private static Settings settings(JourneyViewState.Mode mode) {
        JourneyViewState.Mode effective = owned(mode) ? mode : JourneyViewState.Mode.RESEARCHED;
        Settings settings = values.get(effective);
        if (settings == null) {
            settings = new Settings();
            values.put(effective, settings);
        }
        return settings;
    }

    private static boolean owned(JourneyViewState.Mode mode) {
        return mode == JourneyViewState.Mode.RESEARCHED || mode == JourneyViewState.Mode.FAVOURITE
            || mode == JourneyViewState.Mode.CREATIVE || mode == JourneyViewState.Mode.DELETE;
    }

    private static void resetInternal() {
        values.clear();
        values.put(JourneyViewState.Mode.RESEARCHED, new Settings());
        values.put(JourneyViewState.Mode.FAVOURITE, new Settings());
        values.put(JourneyViewState.Mode.CREATIVE, new Settings());
        values.put(JourneyViewState.Mode.DELETE, new Settings());
    }

    private static final class Settings {
        JourneyGroupMode group = JourneyGroupMode.NONE;
        JourneyOrderMode order = JourneyOrderMode.NONE;
        boolean latest;
    }
}
