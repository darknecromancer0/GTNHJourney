package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class WRCoreRenderBoundaryContractTest {

    @Test
    public void mixinConfigRegistersClientOnlyBoundaryRecovery() throws IOException {
        String config = read("src/main/resources/mixins.gtnhjourney.json");

        assertTrue(config.contains("\"client\""), "render compatibility mixins must be client-only");
        assertTrue(config.contains("\"TessellatorStateAccessor\""));
        assertTrue(config.contains("\"WRCoreEventHandlerMixin\""));
    }

    @Test
    public void wrCoreMixinIsOptionalAndRepairsBeforeWirelessBoltRendering() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/mixin/WRCoreEventHandlerMixin.java");

        assertTrue(source.contains("@Pseudo"), "WR-CBE must remain an optional runtime integration");
        assertTrue(source.contains("codechicken.wirelessredstone.core.WRCoreEventHandler"));
        assertTrue(source.contains("method = \"onRenderWorldLast\""));
        assertTrue(source.contains("at = @At(\"HEAD\")"));
        assertTrue(source.contains("RenderBoundaryRecovery.finishDanglingBatch"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
