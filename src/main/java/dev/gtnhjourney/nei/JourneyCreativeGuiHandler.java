package dev.gtnhjourney.nei;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiAdapter;

/** Restores the NEI item panel on creative tabs where stock GTNH NEI hides it. */
public final class JourneyCreativeGuiHandler extends INEIGuiAdapter {

    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        if (currentVisibility != null && JourneyCreativeVisibilityPolicy.forceItemSection(gui instanceof GuiContainerCreative)) {
            currentVisibility.showItemSection = true;
        }
        return currentVisibility;
    }
}
