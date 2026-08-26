package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneyGlobalCreativeSafetyTest {

    @Test
    public void onlyUnsafeVolumetricFlaskPresentationIsHidden() {
        assertTrue(JourneyGlobalSafetyPolicy.shouldHide("gregtech.common.items.ItemVolumetricFlask", true, true));
        assertFalse(JourneyGlobalSafetyPolicy.shouldHide("gregtech.common.items.ItemVolumetricFlask", true, false));
        assertFalse(JourneyGlobalSafetyPolicy.shouldHide("gregtech.common.items.ItemVolumetricFlask", false, true));
        assertFalse(JourneyGlobalSafetyPolicy.shouldHide("gtPlusPlus.core.item.tool.misc.ItemGregtechPump", true, true));
    }
}
