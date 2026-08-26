package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import net.minecraft.item.ItemStack;

public class FurnaceOutputGateTest {

    @Test
    public void onlyNewNonEmptyIdentityTransitionsFire() {
        FurnaceOutputGate gate = new FurnaceOutputGate();
        gate.prime(0, false);

        assertTrue(gate.observe(12345, true));
        assertFalse(gate.observe(12345, true));
        assertFalse(gate.observe(0, false));
        assertTrue(gate.observe(67890, true));
    }

    @Test
    public void primedExistingOutputDoesNotFabricateCompletion() {
        FurnaceOutputGate gate = new FurnaceOutputGate();
        gate.prime(12345, true);

        assertFalse(gate.observe(12345, true));
        assertFalse(gate.observe(0, false));
        assertTrue(gate.observe(12345, true));
    }

    @Test
    public void trackedFurnaceCounterAdvancesOnlyAfterTheOutputGateAcceptsAChange() throws Exception {
        String source = trackerSource();
        int tickStart = source.indexOf("public void onServerTick");
        int tickEnd = source.indexOf("public void onLogout", tickStart);
        String tickBody = source.substring(tickStart, tickEnd);

        int gate = tickBody.indexOf("if (!state.gate.observe(signature, occupied)) continue;");
        int counter = tickBody.indexOf("JourneyRuntimeCounters.furnaceOutputObservation();");

        assertTrue(gate >= 0);
        assertTrue(counter > gate, "unchanged tracked output must not inflate furnaceOutputObservations");
    }

    @Test
    public void repeatedInteractionBySameOwnerRequiresARealOutputTransition() {
        FurnaceOutputGate gate = new FurnaceOutputGate();

        assertTrue(FurnaceOwnershipTracker.claimInteractionOutput(gate, false, 12345, true));
        assertFalse(FurnaceOwnershipTracker.claimInteractionOutput(gate, true, 12345, true));
        assertTrue(FurnaceOwnershipTracker.claimInteractionOutput(gate, true, 67890, true));

        FurnaceOutputGate differentOwnerGate = new FurnaceOutputGate();
        assertTrue(FurnaceOwnershipTracker.claimInteractionOutput(differentOwnerGate, false, 67890, true));
    }

    @Test
    public void debugResearcherToolInteractionIsExcludedFromFurnaceTracking() throws Exception {
        Method predicate = assertDoesNotThrow(
            () -> FurnaceOwnershipTracker.class.getDeclaredMethod("isDebugResearcherInteraction", ItemStack.class));
        predicate.setAccessible(true);

        ItemStack debugTool = new ItemStack(new ItemDebugResearcherTool(), 1, 0);
        assertTrue((Boolean) predicate.invoke(null, debugTool));
        assertFalse((Boolean) predicate.invoke(null, new Object[] { null }));
    }

    private static String trackerSource() throws Exception {
        return new String(
            Files.readAllBytes(Paths.get("src/main/java/dev/gtnhjourney/acquisition/FurnaceOwnershipTracker.java")),
            StandardCharsets.UTF_8);
    }
}
