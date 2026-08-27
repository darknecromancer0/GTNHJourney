package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.acquisition.ResearchObservationPolicy;
import dev.gtnhjourney.research.ResearchKey;
import gregtech.api.items.MetaGeneratedTool;

public class GtToolStatePolicyTest {

    @Test
    public void registeredGeneratedToolMetaIsVerified() {
        MetaGeneratedTool tool = new MetaGeneratedTool();
        tool.mToolStats.put(Short.valueOf((short) 10), new Object());

        assertTrue(GtToolStatePolicy.isVerifiedTool(new ItemStack(tool, 1, 10)));
    }

    @Test
    public void unregisteredLegacyGeneratedToolMetaIsRejected() {
        MetaGeneratedTool tool = new MetaGeneratedTool();
        tool.mToolStats.put(Short.valueOf((short) 10), new Object());

        assertFalse(GtToolStatePolicy.isVerifiedTool(new ItemStack(tool, 1, 0)));
    }

    @Test
    public void unregisteredLegacyGeneratedToolIsNotObservedButExistingResearchCanStillLoad() {
        MetaGeneratedTool tool = new MetaGeneratedTool();
        tool.mToolStats.put(Short.valueOf((short) 10), new Object());
        ItemStack legacy = new ItemStack(tool, 1, 0);

        assertFalse(ResearchObservationPolicy.shouldObserve(legacy));
        assertNotNull(
            PersistedResearchEntryResolver.resolveReconstructed(
                new ResearchKey("gregtech:gt.metatool.01", 0, ""),
                null,
                legacy));
    }
}
