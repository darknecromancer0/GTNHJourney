package dev.gtnhjourney.backup;

/** Pure cadence policy for automatic world backups. */
public final class WorldBackupPolicy {

    private WorldBackupPolicy() {}

    public static boolean isDue(long nowMillis, long lastSuccessMillis, int intervalSeconds, boolean enabled) {
        if (!enabled || intervalSeconds <= 0 || nowMillis < lastSuccessMillis) return false;
        long intervalMillis = intervalSeconds * 1000L;
        return nowMillis - lastSuccessMillis >= intervalMillis;
    }
}
