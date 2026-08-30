package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release118ContractTest {

    @Test
    public void release118HighSpeedAndBotaniaFeaturesRemainPresent() throws IOException {
        String command = read("src/main/java/dev/gtnhjourney/command/CommandJourney.java");
        String speed = read("src/main/java/dev/gtnhjourney/time/JourneySpeedState.java");
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/MinecraftServerSpeedMixin.java");

        assertTrue(command.contains("/journey botania debug tool"));
        assertTrue(speed.contains("value == 128"));
        assertTrue(mixin.contains("ServerTickPeriodSchedule.fullTicksPerOuterTick"));
        assertTrue(mixin.contains("((MinecraftServer) (Object) this).tick()"));
    }

    @Test
    public void release118HasLiveRegressionChecklist() throws IOException {
        Path path = Paths.get("docs/v1.1.8-live-test.md");
        assertTrue(Files.isRegularFile(path), "missing v1.1.8 live-test checklist");
        String document = read(path.toString()).toLowerCase();
        assertTrue(document.contains("botania mana debug tool"));
        assertTrue(document.contains("128x"));
        assertTrue(document.contains("gregtech"));
        assertTrue(document.contains("vanilla"));
        assertTrue(document.contains("backup"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
