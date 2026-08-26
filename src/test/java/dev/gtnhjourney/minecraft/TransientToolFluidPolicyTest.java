package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TransientToolFluidPolicyTest {

    private static final String GT_PLUS_PLUS_PUMP = "gtPlusPlus.core.item.tool.misc.ItemGregtechPump";

    @Test
    public void pumpDropsDerivedFluidInitializationTagsButKeepsElectricCharge() {
        assertTrue(TransientToolFluidPolicy.isTransientRuntimeKey(GT_PLUS_PLUS_PUMP, "mInit"));
        assertTrue(TransientToolFluidPolicy.isTransientRuntimeKey(GT_PLUS_PLUS_PUMP, "mFluid"));
        assertTrue(TransientToolFluidPolicy.isTransientRuntimeKey(GT_PLUS_PLUS_PUMP, "mFluidAmount"));
        assertTrue(TransientToolFluidPolicy.isTransientRuntimeKey(GT_PLUS_PLUS_PUMP, "mMeta"));
        assertTrue(TransientToolFluidPolicy.isTransientRuntimeKey(GT_PLUS_PLUS_PUMP, "mCapacity"));
        assertTrue(TransientToolFluidPolicy.isTransientRuntimeKey(GT_PLUS_PLUS_PUMP, "capacityInit"));

        assertFalse(TransientToolFluidPolicy.isTransientRuntimeKey(GT_PLUS_PLUS_PUMP, "GT.ItemCharge"));
        assertFalse(TransientToolFluidPolicy.isTransientRuntimeKey(GT_PLUS_PLUS_PUMP, "display"));
    }

    @Test
    public void foreignItemsDoNotInheritPumpRuntimeTagRules() {
        assertFalse(TransientToolFluidPolicy.isTransientRuntimeKey("example.ForeignTool", "mFluid"));
        assertFalse(TransientToolFluidPolicy.isTransientRuntimeKey("example.ForeignTool", "mCapacity"));
    }
}
