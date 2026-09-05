package dev.gtnhjourney.nei;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.guihook.IContainerInputHandler;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.network.Journey1124Network;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/** Journey-owned item views share issuance semantics; the explicit NEI view always keeps native NEI click handling. */
public final class JourneyNEIInputHandler implements IContainerInputHandler {

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        JourneyViewState.Mode mode = JourneyViewState.mode();
        if (mode == JourneyViewState.Mode.ALL) return false;
        if (JourneyViewState.isDelete()) return handleDeleteClick(mousex, mousey, button);

        ItemStack hovered = ItemPanels.itemPanel.getStackMouseOver(mousex, mousey);
        if (hovered == null || hovered.getItem() == null) return false;

        if (JourneyRetrieveClickPolicy.shouldAddFavourite(mode, button, altDown())) {
            setFavourite(hovered, true);
            return true;
        }
        if (JourneyRetrieveClickPolicy.shouldRemoveFavourite(mode, button, altDown())) {
            setFavourite(hovered, false);
            return true;
        }

        if (JourneyViewState.isCreative()) {
            if (!JourneyRetrieveClickPolicy.shouldRetrieve(button, true, controlDown())) return false;
            return handleCreativeIssue(hovered, button, shiftDown());
        }

        if (!JourneyRetrieveClickPolicy.shouldRetrieve(button, true, controlDown())) return false;
        try {
            ResearchKey key = JourneyPresentationKeyResolver.keyOf(hovered);
            if (!ClientResearchMirror.contains(key)) return false;
            if (JourneyRetrieveClickPolicy.shouldFillInventory(button, shiftDown())) {
                JourneyNetwork.requestFillInventory(key);
                return true;
            }
            JourneyNetwork.requestRetrieve(
                key,
                JourneyRetrieveClickPolicy.requestedAmount(button, shiftDown(), hovered.getMaxStackSize()));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void setFavourite(ItemStack hovered, boolean favourite) {
        try {
            ResearchKey key = JourneyPresentationKeyResolver.keyOf(hovered);
            if (ClientResearchMirror.contains(key)) Journey1124Network.requestSet(ResearchFingerprint.of(key), favourite);
        } catch (IllegalArgumentException ignored) {}
    }

    private static boolean handleCreativeIssue(ItemStack hovered, int button, boolean shiftDown) {
        try {
            ResearchKey key = JourneyPresentationKeyResolver.keyOf(hovered);
            if (ClientResearchMirror.contains(key)) {
                if (JourneyRetrieveClickPolicy.shouldFillInventory(button, shiftDown)) JourneyNetwork.requestFillInventory(key);
                else JourneyNetwork.requestRetrieve(
                    key,
                    JourneyRetrieveClickPolicy.requestedAmount(button, shiftDown, hovered.getMaxStackSize()));
                return true;
            }
        } catch (IllegalArgumentException ignored) {}

        boolean fillInventory = JourneyRetrieveClickPolicy.shouldFillInventory(button, shiftDown);
        int amount = JourneyRetrieveClickPolicy.requestedAmount(button, shiftDown, hovered.getMaxStackSize());
        JourneyNetwork.requestCreativeIssue(hovered, amount, fillInventory);
        return true;
    }

    private static boolean handleDeleteClick(int mousex, int mousey, int button) {
        ItemStack hovered = ItemPanels.itemPanel.getStackMouseOver(mousex, mousey);
        if (hovered == null || hovered.getItem() == null) return false;
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

    private static boolean controlDown() { return NEIClientUtils.controlKey(); }
    private static boolean shiftDown() { return NEIClientUtils.shiftKey(); }
    private static boolean altDown() { return NEIClientUtils.altKey(); }

    @Override public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) { return false; }
    @Override public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}
    @Override public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyID) { return false; }
    @Override public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}
    @Override public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {}
    @Override public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) { return false; }
    @Override public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}
    @Override public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {}
}
