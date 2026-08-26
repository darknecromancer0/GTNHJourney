package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

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
    public void debugResearcherToolInteractionIsExcludedFromFurnaceTracking() throws Exception {
        Method predicate = assertDoesNotThrow(
            () -> FurnaceOwnershipTracker.class.getDeclaredMethod("isDebugResearcherInteraction", ItemStack.class));
        predicate.setAccessible(true);

        ItemStack debugTool = new ItemStack(new ItemDebugResearcherTool(), 1, 0);
        assertTrue((Boolean) predicate.invoke(null, debugTool));
        assertFalse((Boolean) predicate.invoke(null, new Object[] { null }));
    }
}
