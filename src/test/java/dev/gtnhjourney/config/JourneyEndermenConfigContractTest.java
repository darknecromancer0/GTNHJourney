package dev.gtnhjourney.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyEndermenConfigContractTest {

    @Test
    void enableStateIsIndependentAndPersistedInSafetyConfiguration() throws Exception {
        String src = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/config/JourneyConfig.java")),
            StandardCharsets.UTF_8);
        assertTrue(src.contains("private static volatile boolean endermenMoveBlocksEnabled = true;"));
        assertTrue(src.contains("endermenMoveBlocksEnabled = config.getBoolean("));
        assertTrue(src.contains("persistBoolean(\"safety\", \"endermenMoveBlocksEnabled\", enabled)"));
    }

    @Test
    void togglingEndermenDoesNotModifyTheVanillaGameruleOrGlobalExplosionGuard() throws Exception {
        String command = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/command/JourneySafetyCommandHandler.java")),
            StandardCharsets.UTF_8);
        assertTrue(command.contains("JourneyConfig.setEndermenMoveBlocksEnabled(enabled);"));
        String mixin = new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/mixin/EntityEndermanBlockCarryMixin.java")),
            StandardCharsets.UTF_8);
        assertTrue(mixin.contains("rules.getGameRuleBooleanValue(ruleName)"));
        org.junit.jupiter.api.Assertions.assertFalse(mixin.contains("setOrCreateGameRule"));
    }
}
