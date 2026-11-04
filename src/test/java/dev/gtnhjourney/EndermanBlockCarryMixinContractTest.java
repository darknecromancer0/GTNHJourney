package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class EndermanBlockCarryMixinContractTest {

    @Test
    void onlyEndermanGameruleCheckIsRedirectedNotOtherMobBehavior() throws Exception {
        String mixin = new String(Files.readAllBytes(
            Paths.get("src/main/java/dev/gtnhjourney/mixin/EntityEndermanBlockCarryMixin.java")),
            StandardCharsets.UTF_8);
        String mixinConfig = new String(Files.readAllBytes(
            Paths.get("src/main/resources/mixins.gtnhjourney.json")), StandardCharsets.UTF_8);
        assertTrue(mixin.contains("@Mixin(EntityEnderman.class)"));
        assertTrue(mixin.contains("getGameRuleBooleanValue(Ljava/lang/String;)Z"));
        assertTrue(mixin.contains("method = \"onLivingUpdate\""));
        assertTrue(mixin.contains("EndermanBlockCarryPolicy.mayMoveBlocks"));
        assertTrue(mixinConfig.contains("\"EntityEndermanBlockCarryMixin\""));
    }
}
