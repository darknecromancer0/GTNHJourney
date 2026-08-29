package dev.gtnhjourney.command;

import dev.gtnhjourney.time.JourneySpeedState;

/** Parsing and permission shape for the session-only /journey speed command. */
public final class JourneySpeedCommandPolicy {

    private JourneySpeedCommandPolicy() {}

    public static boolean requiresAdmin(String[] args) {
        if (args == null || args.length < 2) return false;
        if (!"speed".equalsIgnoreCase(args[0])) return false;
        return !"status".equalsIgnoreCase(args[1]);
    }

    public static Integer parseMultiplier(String value) {
        if (value == null) return null;
        try {
            int parsed = Integer.parseInt(value);
            return JourneySpeedState.isAllowedMultiplier(parsed) ? Integer.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
