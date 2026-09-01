package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** Prevents Journey and GTNH ServerUtilities from independently owning WorldServer.levelSaving. */
class WorldBackupNativeOwnerContractTest {

    @Test
    void serverUtilitiesPresenceDelegatesAllJourneyBackupStartsBeforeWorldMutation() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/backup/WorldBackupCoordinator.java");

        assertTrue(
            source.contains("Loader.isModLoaded(\"serverutilities\")"),
            "GTNH Journey must detect the native ServerUtilities backup owner");
        assertTrue(
            source.contains("nativeBackupOwnerActive()"),
            "backup coordinator must expose/use one explicit native-owner policy");

        int automaticDue = source.indexOf("public boolean isAutomaticDue()");
        int tryBackup = source.indexOf("public synchronized WorldBackupResult tryBackup", automaticDue);
        assertTrue(automaticDue >= 0 && tryBackup > automaticDue, "automatic and manual backup entrypoints must exist");
        String automaticBody = source.substring(automaticDue, tryBackup);
        assertTrue(
            automaticBody.contains("!settings.nativeBackupOwnerActive()"),
            "automatic Journey backups must be rejected when ServerUtilities owns backups");

        int manualNativeGuard = source.indexOf("settings.nativeBackupOwnerActive()", tryBackup);
        int prepare = source.indexOf("prepared = operation.prepare(server)", tryBackup);
        assertTrue(
            manualNativeGuard > tryBackup && prepare > manualNativeGuard,
            "manual Journey backups must also be rejected before any world save flag can be touched");
        assertTrue(
            source.contains("Backup delegated to GTNH ServerUtilities"),
            "the rejection must explain which subsystem owns backups instead of silently skipping them");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
