package dev.gtnhjourney.time;

import java.util.Locale;

/** Defines whether Journey accelerates only tickable block entities or the complete server world. */
public enum JourneySpeedMode {
    MACHINES("machines"),
    WORLD("world");

    private final String commandName;

    JourneySpeedMode(String commandName) {
        this.commandName = commandName;
    }

    public String commandName() {
        return commandName;
    }

    public static JourneySpeedMode parse(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (JourneySpeedMode mode : values()) {
            if (mode.commandName.equals(normalized)) return mode;
        }
        return null;
    }
}
