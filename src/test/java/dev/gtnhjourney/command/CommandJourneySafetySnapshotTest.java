package dev.gtnhjourney.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class CommandJourneySafetySnapshotTest {

    @Test
    public void pruneMissingCreatesSafetySnapshotBeforeBulkDelete() throws Exception {
        String source = compact(commandSource());
        int action = source.indexOf("if (\"prune-missing\".equals(action))");
        int candidates = source.indexOf("List<ResearchKey> unavailable = unavailableKeys(player);", action);
        int safety = source.indexOf("GTNHJourney.MUTATIONS.createSafetySnapshot(player, \"before-prune-missing\");", action);
        int delete = source.indexOf("GTNHJourney.MUTATIONS.deleteMany(player, unavailable", action);

        assertTrue(action >= 0 && candidates > action);
        assertTrue(safety > candidates && safety < delete, "prune-missing must snapshot the current research before deletion");
    }

    @Test
    public void clearCreatesSafetySnapshotBeforeBulkDelete() throws Exception {
        String source = compact(commandSource());
        int action = source.indexOf("if (\"clear\".equals(action))");
        int candidates = source.indexOf("List<ResearchKey> keys = new ArrayList<ResearchKey>(GTNHJourney.RESEARCH.snapshot(player));", action);
        int safety = source.indexOf("GTNHJourney.MUTATIONS.createSafetySnapshot(player, \"before-clear\");", action);
        int delete = source.indexOf("GTNHJourney.MUTATIONS.deleteMany(player, keys", action);

        assertTrue(action >= 0 && candidates > action);
        assertTrue(safety > candidates && safety < delete, "clear must snapshot the current research before deletion");
    }

    private static String commandSource() throws Exception {
        return new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/command/CommandJourney.java")),
            StandardCharsets.UTF_8);
    }

    private static String compact(String source) {
        return source == null ? "" : source.replaceAll("\\s+", " ").trim();
    }
}
