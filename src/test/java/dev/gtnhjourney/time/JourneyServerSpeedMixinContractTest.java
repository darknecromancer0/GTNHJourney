package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneyServerSpeedMixinContractTest {

    @Test
    public void buildEnablesJourneyMixinPackage() throws IOException {
        String properties = read("gradle.properties");
        assertTrue(properties.contains("usesMixins = true"));
        assertTrue(properties.contains("mixinsPackage = mixin"));
    }

    @Test
    public void configRegistersTheServerSpeedMixin() throws IOException {
        String json = read("src/main/resources/mixins.gtnhjourney.json");
        assertTrue(json.contains("\"package\": \"dev.gtnhjourney.mixin\""));
        assertTrue(json.contains("\"MinecraftServerSpeedMixin\""));
        assertTrue(json.contains("mixins.gtnhjourney.refmap.json"));
    }

    @Test
    public void mixinChangesCadenceAndHighSpeedUsesWholeMinecraftServerTicks() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/mixin/MinecraftServerSpeedMixin.java");
        assertTrue(source.contains("@Mixin(MinecraftServer.class)"));
        assertTrue(source.contains("@ModifyConstant"));
        assertTrue(source.contains("longValue = 50L"));
        assertTrue(source.contains("ordinal = 1"));
        assertTrue(source.contains("ordinal = 2"));
        assertTrue(source.contains("ordinal = 3"));
        assertTrue(source.contains("ServerTickPeriodSchedule.periodMillis"));
        assertTrue(source.contains("ServerTickPeriodSchedule.fullTicksPerOuterTick"));
        assertTrue(source.contains("((MinecraftServer) (Object) this).tick()"));
        assertTrue(source.contains("gtnhjourney$insideBurst"));
        assertTrue(source.contains("gtnhjourney$isSpeedHookAvailable"));
        assertTrue(source.contains("gtnhjourney$setSpeedMultiplier"));
        assertTrue(source.contains("gtnhjourney$resetSpeedMultiplier"));
        assertFalse(source.contains("updateEntity()"));
        assertFalse(source.contains("TileEntity"));
        assertFalse(source.contains("gregtech"));
        assertFalse(source.contains("Botania"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
