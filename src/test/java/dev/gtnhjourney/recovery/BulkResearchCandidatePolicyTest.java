package dev.gtnhjourney.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BulkResearchCandidatePolicyTest {

    @Test
    public void bulkMigrationUsesTheSameResearchabilityGateAsOrdinaryObservation() {
        assertFalse(BulkResearchCandidatePolicy.shouldObserve(new ItemStack(new ItemDebugResearcherTool(), 1, 0)));
        assertTrue(BulkResearchCandidatePolicy.shouldObserve(new ItemStack(new Item(), 1, 0)));
    }
}
