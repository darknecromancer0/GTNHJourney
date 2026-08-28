package dev.gtnhjourney.command;

/** Pure parser/permission policy for Journey world-safety command families. */
final class JourneySafetyCommandPolicy {

    private JourneySafetyCommandPolicy() {}

    static boolean isBackupAction(String action) {
        return "status".equals(action) || "now".equals(action) || "on".equals(action) || "off".equals(action);
    }

    static boolean isExplosionAction(String action) {
        return "status".equals(action) || "on".equals(action) || "off".equals(action);
    }

    static boolean requiresAdmin(String family, String action) {
        if ("backup".equals(family)) return !"status".equals(action);
        if ("explosions".equals(family)) return !"status".equals(action);
        return false;
    }
}
