package dev.gtnhjourney.command;

/** Formats short, scoped command errors instead of dumping the complete Journey help page. */
public final class JourneyCommandErrorPolicy {

    public static final String ROOT_CHOICES =
        "help|count|stats|inspect|research|rescan|list|newest|get|forget|undo|redo|restore-deleted|snapshot|snapshots|restore|backup|explosions|cleanse|speed|botania|debug|trace|dump|hotspots|debugtool|prune-missing|clear|return|death";

    private JourneyCommandErrorPolicy() {}

    public static String invalidRoot(String value) {
        return "Invalid Journey command '" + safe(value) + "'. Try: /journey [" + ROOT_CHOICES + "]";
    }

    public static String invalid(String path, String value, String choices) {
        String scope = path == null ? "" : path.trim();
        return "Invalid " + scope + " command '" + safe(value) + "'. Try: /journey " + scope + " [" + choices + "]";
    }

    private static String safe(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.replace('\n', ' ').replace('\r', ' ');
    }
}
