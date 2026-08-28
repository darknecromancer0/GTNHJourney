package dev.gtnhjourney.backup;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Deterministic path, naming and retention helpers for world backups. */
public final class WorldBackupPaths {

    private static final String BACKUP_PREFIX = "backup-";
    private static final String BACKUP_SUFFIX = ".zip";

    private WorldBackupPaths() {}

    public static File backupRoot(File instanceRoot, String worldName) {
        File root = instanceRoot == null ? new File(".") : instanceRoot;
        return new File(new File(root, "gtnhjourney-backups"), safeWorldName(worldName));
    }

    public static String safeWorldName(String worldName) {
        String raw = worldName == null ? "world" : worldName.trim();
        if (raw.length() == 0) raw = "world";
        StringBuilder safe = new StringBuilder(Math.min(raw.length(), 80));
        for (int i = 0; i < raw.length() && safe.length() < 80; i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.'
                || c == '_' || c == '-') {
                safe.append(c);
            } else {
                safe.append('_');
            }
        }
        return safe.length() == 0 ? "world" : safe.toString();
    }

    public static File finalArchive(File backupDir, Date timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date effective = timestamp == null ? new Date() : timestamp;
        return new File(backupDir, BACKUP_PREFIX + format.format(effective) + BACKUP_SUFFIX);
    }

    public static File temporaryArchive(File finalArchive) {
        return new File(finalArchive.getParentFile(), finalArchive.getName() + ".tmp");
    }

    public static List<File> successfulArchives(File backupDir) {
        if (backupDir == null || !backupDir.isDirectory()) return Collections.emptyList();
        File[] files = backupDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname == null || !pathname.isFile()) return false;
                String name = pathname.getName();
                return name.startsWith(BACKUP_PREFIX) && name.endsWith(BACKUP_SUFFIX);
            }
        });
        if (files == null || files.length == 0) return Collections.emptyList();
        List<File> result = new ArrayList<File>(files.length);
        Collections.addAll(result, files);
        Collections.sort(result, new Comparator<File>() {

            @Override
            public int compare(File left, File right) {
                return left.getName().compareTo(right.getName());
            }
        });
        return result;
    }

    public static List<File> rotationVictims(List<File> successfulArchives, int retention) {
        if (successfulArchives == null || successfulArchives.isEmpty()) return Collections.emptyList();
        int boundedRetention = Math.max(1, retention);
        List<File> sorted = new ArrayList<File>(successfulArchives);
        Collections.sort(sorted, new Comparator<File>() {

            @Override
            public int compare(File left, File right) {
                return left.getName().compareTo(right.getName());
            }
        });
        int victimCount = Math.max(0, sorted.size() - boundedRetention);
        if (victimCount == 0) return Collections.emptyList();
        return new ArrayList<File>(sorted.subList(0, victimCount));
    }
}
