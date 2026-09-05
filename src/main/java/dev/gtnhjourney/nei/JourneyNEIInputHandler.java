package dev.gtnhjourney.nei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.guihook.IContainerInputHandler;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.network.Journey1124Network;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/** Journey-owned views reserve LMB for favourites and use RMB/Shift+RMB for issuance. */
public final class JourneyNEIInputHandler implements IContainerInputHandler {

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        if (JourneyViewState.isDelete()) return handleDeleteClick(mousex, mousey, button);
        JourneyViewState.Mode mode = JourneyViewState.mode();
        ItemStack hovered = ItemPanels.itemPanel.getStackMouseOver(mousex, mousey);

        if (JourneyViewState.isEnabled() && button == 0) {
            if (hovered == null || hovered.getItem() == null) return false;
            if (JourneyRetrieveClickPolicy.shouldToggleFavourite(mode, button, altDown())) toggleFavourite(hovered);
            return true;
        }

        if (JourneyViewState.isCreative()) {
            if (hovered == null || hovered.getItem() == null || button != 1) return false;
            return handleCreativeIssue(hovered, shiftDown());
        }

        if (!JourneyRetrieveClickPolicy.shouldRetrieve(button, JourneyViewState.isEnabled(), controlDown())) return false;
        if (hovered == null || hovered.getItem() == null) return false;
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

    private static void toggleFavourite(ItemStack hovered) {
        try {
            ResearchKey key = JourneyPresentationKeyResolver.keyOf(hovered);
            if (ClientResearchMirror.contains(key)) Journey1124Network.requestToggle(ResearchFingerprint.of(key));
        } catch (IllegalArgumentException ignored) {}
    }

    private static boolean handleCreativeIssue(ItemStack hovered, boolean fillInventory) {
        try {
            ResearchKey key = JourneyPresentationKeyResolver.keyOf(hovered);
            if (ClientResearchMirror.contains(key)) {
                if (fillInventory) JourneyNetwork.requestFillInventory(key);
                else JourneyNetwork.requestRetrieve(key, Math.max(1, hovered.getMaxStackSize()));
                return true;
            }
        } catch (IllegalArgumentException ignored) {}

        if (!NEIClientConfig.canCheatItem(hovered)) return true;
        ItemStack give = hovered.copy();
        give.stackSize = Math.max(1, hovered.getMaxStackSize());
        if (!fillInventory) {
            NEIClientUtils.giveStack(give);
            return true;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) return true;
        int empty = 0;
        ItemStack[] main = minecraft.thePlayer.inventory.mainInventory;
        for (int i = 0; i < main.length; i++) if (main[i] == null) empty++;
        for (int i = 0; i < empty; i++) NEIClientUtils.giveStack(give.copy());
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
