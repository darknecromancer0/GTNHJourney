package dev.gtnhjourney.backup;

import java.io.File;

/** Immutable outcome of one attempted world backup. */
public final class WorldBackupResult {

    private final boolean success;
    private final File archive;
    private final String message;

    private WorldBackupResult(boolean success, File archive, String message) {
        this.success = success;
        this.archive = archive;
        this.message = message == null ? "" : message;
    }

    public static WorldBackupResult success(File archive) {
        return new WorldBackupResult(true, archive, archive == null ? "Backup completed." : "Backup completed: " + archive.getPath());
    }

    public static WorldBackupResult failure(String message) {
        return new WorldBackupResult(false, null, message == null || message.length() == 0 ? "Backup failed safely." : message);
    }

    public boolean isSuccess() {
        return success;
    }

    public File getArchive() {
        return archive;
    }

    public String getMessage() {
        return message;
    }
}
