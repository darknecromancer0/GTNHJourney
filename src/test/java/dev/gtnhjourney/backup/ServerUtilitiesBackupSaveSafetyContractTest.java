package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class ServerUtilitiesBackupSaveSafetyContractTest {

    @Test
    void serverStoppingRepairsNativeSaveSuppressionBeforeJourneyShutdown() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");

        int stopping = source.indexOf("public void serverStopping(FMLServerStoppingEvent event)");
        int repair = source.indexOf("ServerUtilitiesBackupSaveSafety.prepareForServerStopping()", stopping);
        int journeyFinish = source.indexOf("WORLD_BACKUPS.finishForShutdown()", stopping);

        assertTrue(stopping >= 0, "server stopping hook must exist");
        assertTrue(repair > stopping, "server stopping must invoke the native backup save-safety guard");
        assertTrue(
            journeyFinish > repair,
            "native levelSaving suppression must be repaired before Journey performs its own shutdown backup cleanup");
    }

    @Test
    void helperReflectsServerUtilitiesWithoutAddingHardDependency() throws IOException {
        Path path = Paths.get("src/main/java/dev/gtnhjourney/backup/ServerUtilitiesBackupSaveSafety.java");
        assertTrue(Files.exists(path), "native backup save-safety helper must exist");
        String source = read(path.toString());

        assertTrue(
            source.contains("serverutils.task.backup.BackupTask"),
            "helper must target the ServerUtilities BackupTask that owns levelSaving");
        assertTrue(source.contains("\"thread\""), "helper must inspect the native backup worker");
        assertTrue(source.contains("\"dimSaveStates\""), "helper must restore ServerUtilities' captured save states");
        assertTrue(source.contains("world.levelSaving ="), "helper must restore WorldServer.levelSaving before final save");
        assertFalse(source.contains("import serverutils."), "compatibility guard must not hard-link ServerUtilities classes");
    }

    @Test
    void liveNativeWorkerHasBoundedShutdownWaitAndFailSafeRestore() throws IOException {
        Path path = Paths.get("src/main/java/dev/gtnhjourney/backup/ServerUtilitiesBackupSaveSafety.java");
        assertTrue(Files.exists(path), "native backup save-safety helper must exist");
        String source = read(path.toString());

        assertTrue(source.contains("join("), "Save & Quit must wait for an in-flight native backup worker");
        assertTrue(source.contains("interrupt()"), "cooperative/newer native workers should be asked to stop");
        assertTrue(
            source.contains("restoreCapturedSaveStates"),
            "shutdown must restore captured save state even if native backup cleanup never runs");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
