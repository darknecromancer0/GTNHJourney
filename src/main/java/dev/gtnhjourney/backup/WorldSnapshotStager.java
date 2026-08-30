package dev.gtnhjourney.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Copies one flushed world into an immutable staging tree while the server thread is paused. */
final class WorldSnapshotStager {

    private static final String STAGING_DIRECTORY = ".staging";

    private WorldSnapshotStager() {}

    static StagedSnapshot stage(File worldDir, File backupRoot) throws IOException {
        if (worldDir == null || !worldDir.isDirectory()) throw new IOException("World directory is unavailable.");
        if (backupRoot == null) throw new IOException("Backup directory is unavailable.");

        String worldPath = worldDir.getCanonicalPath();
        String backupPath = backupRoot.getCanonicalPath();
        if (backupPath.equals(worldPath) || backupPath.startsWith(worldPath + File.separator)) {
            throw new IOException("Backup staging directory must be outside the live world directory.");
        }
        if (!backupRoot.isDirectory() && !backupRoot.mkdirs()) {
            throw new IOException("Cannot create backup directory: " + backupRoot.getPath());
        }

        File stagingBase = new File(backupRoot, STAGING_DIRECTORY);
        if (!stagingBase.isDirectory() && !stagingBase.mkdirs()) {
            throw new IOException("Cannot create staging directory: " + stagingBase.getPath());
        }
        cleanupStaleSnapshots(stagingBase);

        File snapshotRoot = new File(stagingBase, "snapshot-" + UUID.randomUUID().toString());
        File snapshotWorld = new File(snapshotRoot, worldDir.getName());
        try {
            copyTree(worldDir.toPath(), snapshotWorld.toPath());
            return new StagedSnapshot(snapshotRoot, snapshotWorld);
        } catch (IOException failure) {
            deleteTreeQuietly(snapshotRoot);
            throw failure;
        } catch (RuntimeException failure) {
            deleteTreeQuietly(snapshotRoot);
            throw failure;
        }
    }

    private static void cleanupStaleSnapshots(File stagingBase) throws IOException {
        File[] stale = stagingBase.listFiles();
        if (stale == null) throw new IOException("Cannot list staging directory: " + stagingBase.getPath());
        for (File child : stale) {
            deleteTree(child.toPath());
        }
    }

    private static void copyTree(Path source, Path target) throws IOException {
        if (Files.isSymbolicLink(source)) throw new IOException("World snapshot cannot follow symbolic links: " + source);
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            File[] children = source.toFile().listFiles();
            if (children == null) throw new IOException("Cannot list world directory: " + source);
            for (File child : children) {
                copyTree(child.toPath(), target.resolve(child.getName()));
            }
            return;
        }
        if (!Files.isRegularFile(source)) return;
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteTree(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;
        if (Files.isDirectory(path) && !Files.isSymbolicLink(path)) {
            File[] children = path.toFile().listFiles();
            if (children == null) throw new IOException("Cannot list staging directory: " + path);
            for (File child : children) {
                deleteTree(child.toPath());
            }
        }
        Files.deleteIfExists(path);
    }

    private static void deleteTreeQuietly(File root) {
        if (root == null) return;
        try {
            deleteTree(root.toPath());
        } catch (IOException ignored) {
            // Stale staging data is never treated as a valid backup and can be removed by a later cleanup.
        }
    }

    static final class StagedSnapshot {

        private final File snapshotRoot;
        private final File worldDirectory;

        private StagedSnapshot(File snapshotRoot, File worldDirectory) {
            this.snapshotRoot = snapshotRoot;
            this.worldDirectory = worldDirectory;
        }

        File worldDirectory() {
            return worldDirectory;
        }

        void cleanup() throws IOException {
            deleteTree(snapshotRoot.toPath());
            File stagingBase = snapshotRoot.getParentFile();
            if (stagingBase != null) {
                File[] remaining = stagingBase.listFiles();
                if (remaining != null && remaining.length == 0) Files.deleteIfExists(stagingBase.toPath());
            }
        }
    }
}
