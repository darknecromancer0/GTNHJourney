package dev.gtnhjourney.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Synchronously archives one already-saved world directory, then rotates only successful archives. */
public final class WorldArchiveWriter {

    private static final int BUFFER_SIZE = 64 * 1024;
    private final ArchiveOutputFactory outputFactory;

    public WorldArchiveWriter() {
        this(new ArchiveOutputFactory() {

            @Override
            public OutputStream open(File target) throws IOException {
                return new FileOutputStream(target);
            }
        });
    }

    WorldArchiveWriter(ArchiveOutputFactory outputFactory) {
        if (outputFactory == null) throw new IllegalArgumentException("outputFactory");
        this.outputFactory = outputFactory;
    }

    public WorldBackupResult write(File worldDir, File backupDir, String worldName, int retention, Date timestamp) {
        File temporary = null;
        try {
            validateSourceAndDestination(worldDir, backupDir);
            if (!backupDir.isDirectory() && !backupDir.mkdirs()) {
                return WorldBackupResult.failure("Backup failed: cannot create " + backupDir.getPath());
            }

            File target = chooseAvailableFinalArchive(backupDir, timestamp);
            temporary = WorldBackupPaths.temporaryArchive(target);
            Files.deleteIfExists(temporary.toPath());

            writeZip(worldDir, temporary);
            promote(temporary, target);
            temporary = null;
            rotateSuccessfulArchives(backupDir, retention);
            return WorldBackupResult.success(target);
        } catch (IOException failure) {
            deleteTemporaryQuietly(temporary);
            return WorldBackupResult.failure("Backup failed safely: " + safeMessage(failure));
        } catch (RuntimeException failure) {
            deleteTemporaryQuietly(temporary);
            return WorldBackupResult.failure("Backup failed safely: " + safeMessage(failure));
        }
    }

    private static void validateSourceAndDestination(File worldDir, File backupDir) throws IOException {
        if (worldDir == null || !worldDir.isDirectory()) throw new IOException("World directory is unavailable.");
        if (backupDir == null) throw new IOException("Backup directory is unavailable.");

        String worldPath = worldDir.getCanonicalPath();
        String backupPath = backupDir.getCanonicalPath();
        if (backupPath.equals(worldPath) || backupPath.startsWith(worldPath + File.separator)) {
            throw new IOException("Backup directory must be outside the world directory.");
        }
    }

    private File chooseAvailableFinalArchive(File backupDir, Date timestamp) {
        Date candidateTime = timestamp == null ? new Date() : new Date(timestamp.getTime());
        File candidate = WorldBackupPaths.finalArchive(backupDir, candidateTime);
        while (candidate.exists() || WorldBackupPaths.temporaryArchive(candidate).exists()) {
            candidateTime = new Date(candidateTime.getTime() + 1000L);
            candidate = WorldBackupPaths.finalArchive(backupDir, candidateTime);
        }
        return candidate;
    }

    private void writeZip(File worldDir, File temporary) throws IOException {
        OutputStream raw = null;
        ZipOutputStream zip = null;
        try {
            raw = outputFactory.open(temporary);
            zip = new ZipOutputStream(new BufferedOutputStream(raw));
            raw = null;
            addDirectoryContents(worldDir, worldDir, zip);
        } finally {
            if (zip != null) {
                zip.close();
            } else if (raw != null) {
                raw.close();
            }
        }
    }

    private static void addDirectoryContents(File root, File current, ZipOutputStream zip) throws IOException {
        File[] children = current.listFiles();
        if (children == null) throw new IOException("Cannot list world directory: " + current.getPath());
        for (File child : children) {
            if (child.isDirectory()) {
                addDirectoryContents(root, child, zip);
            } else if (child.isFile()) {
                addFile(root, child, zip);
            }
        }
    }

    private static void addFile(File root, File file, ZipOutputStream zip) throws IOException {
        String rootPath = root.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        String prefix = rootPath.endsWith(File.separator) ? rootPath : rootPath + File.separator;
        if (!filePath.startsWith(prefix)) throw new IOException("World file escaped source directory: " + file.getPath());

        String relative = filePath.substring(prefix.length()).replace(File.separatorChar, '/');
        ZipEntry entry = new ZipEntry(relative);
        entry.setTime(file.lastModified());
        zip.putNextEntry(entry);
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) zip.write(buffer, 0, read);
            }
        } finally {
            zip.closeEntry();
        }
    }

    private static void promote(File temporary, File target) throws IOException {
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary.toPath(), target.toPath());
        }
    }

    private static void rotateSuccessfulArchives(File backupDir, int retention) {
        List<File> victims = WorldBackupPaths.rotationVictims(
            WorldBackupPaths.successfulArchives(backupDir),
            Math.max(1, retention));
        for (File victim : victims) {
            try {
                Files.deleteIfExists(victim.toPath());
            } catch (IOException ignored) {
                // The new backup is already complete. A stale old archive is safer than invalidating it.
            }
        }
    }

    private static void deleteTemporaryQuietly(File temporary) {
        if (temporary == null) return;
        try {
            Files.deleteIfExists(temporary.toPath());
        } catch (IOException ignored) {
            // A leftover .tmp is ignored by rotation and can be replaced by a later attempt.
        }
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.length() == 0 ? failure.getClass().getSimpleName() : message;
    }

    interface ArchiveOutputFactory {

        OutputStream open(File target) throws IOException;
    }
}
