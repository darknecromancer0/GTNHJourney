package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldBackupPathsTest {

    @TempDir
    File temp;

    @Test
    void backupRootStaysOutsideWorldDirectory() throws Exception {
        File world = new File(temp, "saves/My World");
        assertTrue(world.mkdirs());
        File root = WorldBackupPaths.backupRoot(temp, "My World");
        assertFalse(root.getCanonicalPath().startsWith(world.getCanonicalPath() + File.separator));
        assertEquals(new File(temp, "gtnhjourney-backups/My_World").getCanonicalFile(), root.getCanonicalFile());
    }

    @Test
    void tempArchivesNeverCountAsSuccessfulAndOldestRotatesFirst() throws Exception {
        File dir = new File(temp, "backups");
        assertTrue(dir.mkdirs());
        File first = new File(dir, "backup-2026-08-28_01-00-00.zip");
        File second = new File(dir, "backup-2026-08-28_01-05-00.zip");
        File third = new File(dir, "backup-2026-08-28_01-10-00.zip");
        File fourth = new File(dir, "backup-2026-08-28_01-15-00.zip");
        assertTrue(first.createNewFile());
        assertTrue(second.createNewFile());
        assertTrue(third.createNewFile());
        assertTrue(fourth.createNewFile());
        assertTrue(new File(dir, "backup-2026-08-28_01-20-00.zip.tmp").createNewFile());

        List<File> successful = WorldBackupPaths.successfulArchives(dir);
        assertEquals(4, successful.size());
        List<File> victims = WorldBackupPaths.rotationVictims(successful, 3);
        assertEquals(1, victims.size());
        assertEquals(first.getCanonicalFile(), victims.get(0).getCanonicalFile());
    }

    @Test
    void archiveNamesAreSortableAndTemporaryNameIsDistinct() {
        File dir = new File(temp, "backups");
        File archive = WorldBackupPaths.finalArchive(dir, new Date(0L));
        assertTrue(archive.getName().matches("backup-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.zip"));
        assertEquals(archive.getName() + ".tmp", WorldBackupPaths.temporaryArchive(archive).getName());
    }
}
