package dev.gtnhjourney.time;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class MachineTickAcceleratorContractTest {

    @Test
    public void machinesModeTicksOnlyLoadedTileEntities() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/time/MachineTickAccelerator.java");

        assertTrue(source.contains("ServerTickEvent"));
        assertTrue(source.contains("Phase.END"));
        assertTrue(source.contains("loadedTileEntityList"));
        assertTrue(source.contains("canUpdate()"));
        assertTrue(source.contains("updateEntity()"));
        assertTrue(source.contains("JourneySpeedMode.MACHINES"));
        assertFalse(source.contains("tickBlocksAndAmbiance"));
        assertFalse(source.contains("updateEntities()"));
        assertFalse(source.contains("setWorldTime"));
    }

    @Test
    public void workBudgetCanStopOnlyBetweenCompleteGlobalPasses() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/time/MachineTickAccelerator.java");

        assertTrue(source.contains("tickCompletePass(snapshots);"));
        assertTrue(source.contains("if (pass > 0 && System.nanoTime() >= deadline) return;"));
        int completePassMethod = source.indexOf("private static void tickCompletePass");
        assertTrue(completePassMethod >= 0);
        String passBody = source.substring(completePassMethod);
        assertFalse(passBody.contains("System.nanoTime() >= deadline"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
