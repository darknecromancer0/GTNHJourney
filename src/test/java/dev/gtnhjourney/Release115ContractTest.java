package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Release115ContractTest {

    @Test
    public void release115KeepsWrCbeRenderRegressionCoverage() throws IOException {
        String liveTest = read("docs/v1.1.5-live-test.md");
        String mixin = read("src/main/java/dev/gtnhjourney/mixin/WRCoreEventHandlerMixin.java");

        assertTrue(liveTest.contains("Already tesselating!"));
        assertTrue(liveTest.contains("WR-CBE"));
        assertTrue(mixin.contains("RenderBoundaryRecovery.finishDanglingBatch"));
        assertTrue(mixin.contains("onRenderWorldLast"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
