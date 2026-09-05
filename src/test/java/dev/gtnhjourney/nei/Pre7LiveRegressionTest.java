package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.acquisition.FurnaceOutputGate;
import dev.gtnhjourney.acquisition.ResearchObservationPolicy;
import dev.gtnhjourney.client.UnlockNotificationText;
import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import dev.gtnhjourney.minecraft.ElectricItemDamagePolicy;
import dev.gtnhjourney.minecraft.TransientToolFluidPolicy;
import dev.gtnhjourney.network.UnlockNotificationPolicy;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class Pre7LiveRegressionTest {

    @Test
    public void latestModifierKeepsTheFullJSetButUsesMeaningfulActivityOrder() {
        JourneySortEntry log = entry("test:log", 1, 30);
        JourneySortEntry stone = entry("test:stone", 2, 10);
        JourneySortEntry potato = entry("test:potato", 3, 20);

        List<JourneySortEntry> result = JourneySortPlanner.sort(
            Arrays.asList(log, stone, potato),
            JourneyGroupMode.NONE,
            JourneyOrderMode.NONE,
            true);

        assertEquals(Arrays.asList(log, potato, stone), result);
    }

    @Test
    public void openingAFurnaceWithExistingOutputClaimsThatOutputOnce() {
        FurnaceOutputGate gate = new FurnaceOutputGate();
        assertTrue(gate.claim(12345, true));
        assertFalse(gate.observe(12345, true));
    }

    @Test
    public void emptyFurnaceClaimDoesNotFabricateAnUnlock() {
        FurnaceOutputGate gate = new FurnaceOutputGate();
        assertFalse(gate.claim(0, false));
    }

    @Test
    public void debugResearcherToolIsNeverResearchable() {
        ItemStack debugTool = new ItemStack(new ItemDebugResearcherTool(), 1, 0);
        ItemStack ordinary = new ItemStack(new Item(), 1, 0);

        assertFalse(ResearchObservationPolicy.shouldObserve(debugTool));
        assertTrue(ResearchObservationPolicy.shouldObserve(ordinary));
    }

    @Test
    public void debugToolClientBlockUseMustNotConsumeBeforeServerExecution() {
        assertFalse(ItemDebugResearcherTool.consumeClientBlockUse());
    }

    @Test
    public void gtPlusPlusPumpFluidPayloadIsTransientToolState() {
        assertTrue(TransientToolFluidPolicy.isTransientFluidToolClassName("gtPlusPlus.core.item.tool.misc.ItemGregtechPump"));
        assertFalse(TransientToolFluidPolicy.isTransientFluidToolClassName("gregtech.common.items.ItemVolumetricFlask"));
    }

    @Test
    public void dischargedElectricToolUsesOneStableEmptyDamageValue() {
        assertEquals(27, ElectricItemDamagePolicy.emptyDamage(27, 26));
        assertEquals(27, ElectricItemDamagePolicy.emptyDamage(27, 12));
        assertEquals(0, ElectricItemDamagePolicy.emptyDamage(0, 0));
    }

    @Test
    public void oneLogicalObservationProducesOneHumanNotification() {
        assertFalse(UnlockNotificationPolicy.shouldNotify(0));
        assertTrue(UnlockNotificationPolicy.shouldNotify(1));
        assertTrue(UnlockNotificationPolicy.shouldNotify(2));
        assertEquals("Unlocked: Potato", UnlockNotificationText.format("Potato"));
    }

    @Test
    public void journeyForcesTheNeiItemSectionVisibleInCreativeScreens() {
        assertTrue(JourneyCreativeVisibilityPolicy.forceItemSection(true));
        assertFalse(JourneyCreativeVisibilityPolicy.forceItemSection(false));
    }

    private static JourneySortEntry entry(String id, long unlock, long activity) {
        ResearchKey key = new ResearchKey(id, 0, "");
        return new JourneySortEntry(
            key, null, (int) unlock, id, "test", "misc", "misc", id, unlock, activity, 0L, (int) unlock);
    }
}
