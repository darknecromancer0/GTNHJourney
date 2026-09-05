package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release1124ContractTest {

    @Test
    public void releaseMetadataIs1124Everywhere() throws IOException {
        String runtime = read("src/main/java/dev/gtnhjourney/GTNHJourney.java");
        String gradle = read("build.gradle.kts");
        String mcmod = read("src/main/resources/mcmod.info");
        assertTrue(runtime.contains("public static final String VERSION = \"1.1.24\";"));
        assertTrue(gradle.contains("version = \"1.1.24\""));
        assertTrue(mcmod.contains("\"version\": \"1.1.24\""));
    }

    @Test
    public void favouriteGesturesAreDirectionalAndNormalIssuanceRemains() throws IOException {
        String policy = read("src/main/java/dev/gtnhjourney/nei/JourneyRetrieveClickPolicy.java");
        String input = read("src/main/java/dev/gtnhjourney/nei/JourneyNEIInputHandler.java");
        String network = read("src/main/java/dev/gtnhjourney/network/Journey1124Network.java");

        assertTrue(policy.contains("shouldAddFavourite"));
        assertTrue(policy.contains("JourneyViewState.Mode.RESEARCHED"));
        assertTrue(policy.contains("JourneyViewState.Mode.NEWEST"));
        assertTrue(policy.contains("shouldRemoveFavourite"));
        assertTrue(policy.contains("JourneyViewState.Mode.FAVOURITE"));
        assertTrue(policy.contains("if (journeyView) return button == 0 || button == 1;"));
        assertTrue(input.contains("setFavourite(hovered, true)"));
        assertTrue(input.contains("setFavourite(hovered, false)"));
        assertTrue(network.contains("requestSet(ResearchFingerprint fingerprint, boolean favourite)"));
        assertFalse(network.contains("requestToggle"));
    }

    @Test
    public void machineSpeedIsCappedAndExtraTicksFinishWholePasses() throws IOException {
        String safety = read("src/main/java/dev/gtnhjourney/time/JourneySpeedSafetyPolicy.java");
        String accelerator = read("src/main/java/dev/gtnhjourney/time/MachineTickAccelerator.java");
        String command = read("src/main/java/dev/gtnhjourney/command/CommandJourney1124.java");

        assertTrue(safety.contains("MAX_SAFE_MACHINE_MULTIPLIER = 16"));
        assertTrue(accelerator.contains("tickCompletePass(snapshots);"));
        assertTrue(accelerator.contains("if (pass > 0 && System.nanoTime() >= deadline) return;"));
        assertTrue(command.contains("machines <1|2|4|8|16>"));
        assertTrue(command.contains("world <1|2|4|8|16|32|64|128>"));
    }

    @Test
    public void externalSnapshotRecoveryIsOutsideSavesAndHasLatestReturnPath() throws IOException {
        String archive = read("src/main/java/dev/gtnhjourney/recovery/ExternalJourneySnapshotArchive.java");
        String restore = read("src/main/java/dev/gtnhjourney/recovery/ExternalJourneySnapshotRestoreService.java");
        String command = read("src/main/java/dev/gtnhjourney/command/CommandJourney1124.java");

        assertTrue(archive.contains("gtnhjourney-recovery"));
        assertTrue(archive.contains("research-snapshots"));
        assertTrue(archive.contains("ArchivedSnapshot latest"));
        assertTrue(restore.contains("restoreLatest(EntityPlayerMP player)"));
        assertTrue(restore.contains("Return latest external Journey snapshot"));
        assertFalse(restore.contains("readFromNBT"));
        assertTrue(command.contains("\"snapshot\".equals(action)"));
        assertTrue(command.contains("\"latest\".equalsIgnoreCase(args[1])"));
        assertTrue(command.contains("\"return\".equalsIgnoreCase(args[2])"));
    }

    @Test
    public void invalidJourneyBranchesUseScopedVanillaCommandErrors() throws IOException {
        String command = read("src/main/java/dev/gtnhjourney/command/CommandJourney1124.java");
        String policy = read("src/main/java/dev/gtnhjourney/command/JourneyCommandErrorPolicy.java");
        assertTrue(command.contains("throw commandError(JourneyCommandErrorPolicy.invalidRoot"));
        assertTrue(command.contains("throw commandError(JourneyCommandErrorPolicy.invalid("));
        assertTrue(command.contains("new CommandException(text, new Object[0])"));
        assertTrue(policy.contains("Invalid Journey command"));
        assertTrue(policy.contains("Invalid \" + scope + \" command"));
    }

    @Test
    public void release1124HasLiveRegressionChecklist() throws IOException {
        Path path = Paths.get("docs/v1.1.24-live-test.md");
        assertTrue(Files.isRegularFile(path));
        String text = read(path.toString()).toLowerCase();
        assertTrue(text.contains("alt+lmb"));
        assertTrue(text.contains("alt+rmb"));
        assertTrue(text.contains("ebf"));
        assertTrue(text.contains("gtnhjourney-recovery"));
        assertTrue(text.contains("snapshot latest return"));
        assertTrue(text.contains("red scoped error"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
