package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.gtnhjourney.research.ResearchKey;

class ExternalJourneySnapshotArchiveTest {

    @TempDir
    File temp;

    @Test
    void archiveUsesIndependentRecoveryDirectoryAndHardFiftyMiBBudget() {
        File dir = ExternalJourneySnapshotArchive.archiveDirectory(temp, "Cirno's wonderful world 6");

        assertTrue(dir.getPath().contains("gtnhjourney-recovery"));
        assertTrue(dir.getPath().contains("research-snapshots"));
        assertEquals(50L * 1024L * 1024L, ExternalJourneySnapshotArchive.MAX_ARCHIVE_BYTES);
    }

    @Test
    void writingOneSnapshotCreatesCompressedStandaloneRecoveryFile() throws Exception {
        ResearchStateSnapshot state = new ResearchStateSnapshot(Arrays.asList(
            new ResearchEntrySnapshot(new ResearchKey("minecraft:stone", 0, ""), null, 0),
            new ResearchEntrySnapshot(new ResearchKey("minecraft:diamond", 0, ""), null, 1)));
        UUID player = UUID.fromString("f619f4d3-1b9e-42e1-a187-475eff7e6c0a");

        File written = ExternalJourneySnapshotArchive.write(temp, "Recovery World", player, 123456L, 1788260000000L, state);

        assertTrue(written.isFile());
        assertTrue(written.getName().endsWith(".dat"));
        assertTrue(written.getName().contains("123456"));
        assertTrue(written.length() > 0L);
        assertFalse(new File(written.getParentFile(), written.getName() + ".tmp").exists());
        assertEquals(2, ExternalJourneySnapshotArchive.readEntryCount(written));
    }

    @Test
    void rotationDeletesOldestFilesUntilDirectoryIsWithinBudget() throws Exception {
        File dir = ExternalJourneySnapshotArchive.archiveDirectory(temp, "World");
        assertTrue(dir.mkdirs() || dir.isDirectory());
        File oldest = sized(dir, "snapshot-old.dat", 40, 1000L);
        File middle = sized(dir, "snapshot-middle.dat", 40, 2000L);
        File newest = sized(dir, "snapshot-new.dat", 40, 3000L);

        ExternalJourneySnapshotArchive.pruneToBudget(dir, 80L);

        assertFalse(oldest.exists());
        assertTrue(middle.exists());
        assertTrue(newest.exists());
        assertTrue(ExternalJourneySnapshotArchive.totalBytes(dir) <= 80L);
    }

    private static File sized(File dir, String name, int bytes, long modified) throws IOException {
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(new byte[bytes]);
        }
        assertTrue(file.setLastModified(modified));
        return file;
    }
}
