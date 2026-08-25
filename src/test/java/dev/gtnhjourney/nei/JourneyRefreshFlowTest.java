package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class JourneyRefreshFlowTest {

    @AfterEach
    public void resetMode() {
        JourneyViewState.setMode(JourneyViewState.Mode.ALL);
    }

    @Test
    public void journeyProviderIsAllowAllBecauseControllerAlreadySelectsResearchStates() {
        JourneyItemFilterProvider provider = new JourneyItemFilterProvider();
        ItemStack arbitrary = new ItemStack(new Item(), 1, 0);

        JourneyViewState.setMode(JourneyViewState.Mode.RESEARCHED);
        assertTrue(provider.getFilter().matches(arbitrary));

        JourneyViewState.setMode(JourneyViewState.Mode.NEWEST);
        assertTrue(provider.getFilter().matches(arbitrary));
    }

    @Test
    public void refreshTrackerExposesPanelLifecycleResetInsteadOfVariantCleanup() {
        assertDoesNotThrow(() -> JourneyNEIRefreshTracker.class.getDeclaredMethod("resetJourneyPanel"));
    }
}
