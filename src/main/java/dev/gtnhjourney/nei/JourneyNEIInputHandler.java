package dev.gtnhjourney.nei;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.guihook.IContainerInputHandler;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.research.ResearchKey;

/** Journey modes own researched-panel clicks; D turns plain LMB into exact server-authoritative deletion. */
public final class JourneyNEIInputHandler implements IContainerInputHandler {

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        if (JourneyViewState.isDelete()) return handleDeleteClick(mousex, mousey, button);
        if (!JourneyRetrieveClickPolicy.shouldRetrieve(button, JourneyViewState.isEnabled(), controlDown()))
            return false;
        ItemStack hovered = ItemPanels.itemPanel.getStackMouseOver(mousex, mousey);
        if (hovered == null || hovered.getItem() == null) return false;
        try {
            ResearchKey key = JourneyPresentationKeyResolver.keyOf(hovered);
            if (!ClientResearchMirror.contains(key)) return false;
            int amount = JourneyRetrieveClickPolicy.requestedAmount(button, shiftDown(), hovered.getMaxStackSize());
            JourneyNetwork.requestRetrieve(key, amount);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean handleDeleteClick(int mousex, int mousey, int button) {
        ItemStack hovered = ItemPanels.itemPanel.getStackMouseOver(mousex, mousey);
        if (hovered == null || hovered.getItem() == null) return false;
        // D owns clicks on its researched entries so RMB/Shift cannot fall through into retrieve/give behavior.
        if (!JourneyDeleteClickPolicy.shouldDelete(button, shiftDown())) return true;
        try {
            ResearchKey key = JourneyPresentationKeyResolver.keyOf(hovered);
            if (!ClientResearchMirror.contains(key)) return true;
            JourneyNetwork.requestDelete(key);
            return true;
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }

    private static boolean controlDown() {
        return NEIClientUtils.controlKey();
    }

    private static boolean shiftDown() {
        return NEIClientUtils.shiftKey();
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyID) {
        return false;
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        return false;
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {}
}
