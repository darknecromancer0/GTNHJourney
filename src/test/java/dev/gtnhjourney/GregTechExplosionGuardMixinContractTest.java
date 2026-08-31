package dev.gtnhjourney;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class GregTechExplosionGuardMixinContractTest {

    @Test
    public void directGregTechMachineAndPipeExplosionsAreCancelledWhenJourneyExplosionsAreOff() throws IOException {
        String machineMixin = read("src/main/java/dev/gtnhjourney/mixin/GregTechBaseMetaTileExplosionMixin.java");
        String pipeMixin = read("src/main/java/dev/gtnhjourney/mixin/GregTechBaseMetaPipeExplosionMixin.java");
        String mixins = read("src/main/resources/mixins.gtnhjourney.json");

        assertTrue(machineMixin.contains("gregtech.api.metatileentity.BaseMetaTileEntity"));
        assertTrue(machineMixin.contains("method = \"doExplosion\""));
        assertTrue(machineMixin.contains("JourneyConfig.explosionsEnabled()"));
        assertTrue(machineMixin.contains("ci.cancel()"));

        assertTrue(pipeMixin.contains("gregtech.api.metatileentity.BaseMetaPipeEntity"));
        assertTrue(pipeMixin.contains("method = \"doExplosion\""));
        assertTrue(pipeMixin.contains("JourneyConfig.explosionsEnabled()"));
        assertTrue(pipeMixin.contains("ci.cancel()"));

        assertTrue(mixins.contains("\"GregTechBaseMetaTileExplosionMixin\""));
        assertTrue(mixins.contains("\"GregTechBaseMetaPipeExplosionMixin\""));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
