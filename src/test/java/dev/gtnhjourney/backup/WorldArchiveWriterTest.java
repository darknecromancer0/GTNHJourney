package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldArchiveWriterTest {

    @TempDir
    File temp;

    @Test
    void usesFastDeflateLevelForWorldBackups() {
        assertEquals(Deflater.BEST_SPEED, WorldArchiveWriter.defaultCompressionLevel());
    }

    @Test
    void writesCompleteSaveUnderWorldDirectoryAndKeepsOnlyNewestThreeSuccessfulArchives() throws Exception {
        File world = createWorldTree();
        File backupDir = new File(temp, "gtnhjourney-backups/TestWorld");
        WorldArchiveWriter writer = new WorldArchiveWriter();

        for (int i = 0; i < 4; i++) {
            WorldBackupResult result = writer.write(world, backupDir, "TestWorld", 3, new Date(i * 300_000L));
            assertTrue(result.isSuccess(), result.getMessage());
            assertNotNull(result.getArchive());
        }

        List<File> archives = WorldBackupPaths.successfulArchives(backupDir);
        assertEquals(3, archives.size());
        assertEquals("backup-1970-01-01_00-05-00.zip", archives.get(0).getName());

        String worldFolder = world.getName() + "/";
        try (ZipFile zip = new ZipFile(archives.get(archives.size() - 1))) {
            assertNotNull(zip.getEntry(worldFolder + "level.dat"));
            assertNotNull(zip.getEntry(worldFolder + "region/r.0.0.mca"));
            assertNotNull(zip.getEntry(worldFolder + "DIM-1/region/r.0.0.mca"));
            assertNull(zip.getEntry("level.dat"));
            assertNull(zip.getEntry("region/r.0.0.mca"));
            assertNull(zip.getEntry("DIM-1/region/r.0.0.mca"));
        }
    }

    @Test
    void failedNewBackupPreservesEveryPreviousGoodArchive() throws Exception {
        File world = createWorldTree();
        File backupDir = new File(temp, "backups");
        assertTrue(backupDir.mkdirs());
        for (int i = 0; i < 3; i++) {
            assertTrue(new File(backupDir, "backup-2026-08-28_01-0" + i + "-00.zip").createNewFile());
        }

        WorldArchiveWriter writer = new WorldArchiveWriter(new WorldArchiveWriter.ArchiveOutputFactory() {

            @Override
            public OutputStream open(File target) throws IOException {
                throw new IOException("synthetic write failure");
            }
        });
        WorldBackupResult result = writer.write(world, backupDir, "TestWorld", 3, new Date(1_000_000L));

        assertFalse(result.isSuccess());
        assertEquals(3, WorldBackupPaths.successfulArchives(backupDir).size());
    }

    @Test
    void rejectsBackupDestinationInsideSourceWorld() throws Exception {
        File world = createWorldTree();
        File nestedBackupDir = new File(world, "gtnhjourney-backups/TestWorld");
        WorldBackupResult result = new WorldArchiveWriter().write(world, nestedBackupDir, "TestWorld", 3, new Date(0L));

        assertFalse(result.isSuccess());
        assertFalse(nestedBackupDir.exists());
    }

    private File createWorldTree() throws IOException {
        File world = new File(temp, "world");
        write(new File(world, "level.dat"), "level");
        write(new File(world, "region/r.0.0.mca"), "overworld");
        write(new File(world, "DIM-1/region/r.0.0.mca"), "nether");
        return world;
    }

    private static void write(File file, String value) throws IOException {
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
