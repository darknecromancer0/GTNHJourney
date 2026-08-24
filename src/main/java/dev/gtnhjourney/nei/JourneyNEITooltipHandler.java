package dev.gtnhjourney.nei;

import java.util.List;
import java.util.Map;

import codechicken.nei.ItemPanels;
import codechicken.nei.guihook.IContainerTooltipHandler;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

/** Adds research state and retrieval shortcuts to NEI tooltips without replacing NEI's native UI. */
public final class JourneyNEITooltipHandler implements IContainerTooltipHandler {

    @Override
    public List<String> handleItemTooltip(
        GuiContainer gui,
        ItemStack itemstack,
        int mousex,
        int mousey,
        List<String> currenttip
    ) {
        if (isResearched(itemstack)) {
            currenttip.add(EnumChatFormatting.AQUA + JourneyTooltipText.researchedLine());
        }
        return currenttip;
    }

    @Override
    public Map<String, String> handleHotkeys(GuiContainer gui, int mousex, int mousey, Map<String, String> hotkeys) {
        ItemStack hovered = ItemPanels.itemPanel.getStackMouseOver(mousex, mousey);
        if (!isResearched(hovered)) return hotkeys;
        boolean journeyView = JourneyViewState.isEnabled();
        hotkeys.put(journeyView ? "LMB" : "Ctrl+LMB", JourneyTooltipText.fullStackHint(journeyView));
        hotkeys.put(
            journeyView ? "RMB / Shift+LMB" : "Ctrl+RMB / Ctrl+Shift+LMB",
            JourneyTooltipText.singleItemHint(journeyView));
        return hotkeys;
    }

    private static boolean isResearched(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        try {
            ResearchKey key = ItemStackKeyFactory.from(stack);
            return ClientResearchMirror.contains(key);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
