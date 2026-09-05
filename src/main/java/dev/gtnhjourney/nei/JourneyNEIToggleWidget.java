package dev.gtnhjourney.nei;

import java.util.List;
import java.util.Map;

import codechicken.nei.Button;
import codechicken.nei.ItemPanels;
import codechicken.nei.guihook.IContainerDrawHandler;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.network.JourneyNetwork;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Native-looking J/N/F/C/D/S/T controls in NEI's item-panel header. */
public final class JourneyNEIToggleWidget
    implements IContainerDrawHandler, IContainerInputHandler, IContainerTooltipHandler {

    private final Button researchButton = new Button("J") {
        @Override public boolean onButtonPress(boolean rightclick) { JourneyViewState.toggle(); return true; }
        @Override public String getButtonTip() {
            return JourneyButtonPresentation.researchTooltip(JourneyViewState.mode(), ClientStackMirror.serverOnlyCount());
        }
        @Override public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.RESEARCHED ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button newestButton = new Button("N") {
        @Override public boolean onButtonPress(boolean rightclick) { JourneyViewState.toggleNewest(); return true; }
        @Override public String getButtonTip() { return JourneyButtonPresentation.newestTooltip(JourneyViewState.isNewest()); }
        @Override public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.NEWEST ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button favouriteButton = new Button("F") {
        @Override public boolean onButtonPress(boolean rightclick) { JourneyViewState.toggleFavourite(); return true; }
        @Override public String getButtonTip() { return JourneyButtonPresentation.favouriteTooltip(JourneyViewState.isFavourite()); }
        @Override public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.FAVOURITE ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button creativeButton = new Button("C") {
        @Override public boolean onButtonPress(boolean rightclick) { JourneyViewState.toggleCreative(); return true; }
        @Override public String getButtonTip() { return JourneyButtonPresentation.creativeTooltip(JourneyViewState.isCreative()); }
        @Override public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.CREATIVE ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button deleteButton = new Button("D") {
        @Override public boolean onButtonPress(boolean rightclick) { JourneyViewState.toggleDelete(); return true; }
        @Override public String getButtonTip() {
            return JourneyViewState.isDelete()
                ? "Delete view: plain left-click removes one exact researched state."
                : "Delete view: plain left-click one researched state to forget it. Undo remains available.";
        }
        @Override public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.DELETE ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button scanButton = new Button("S") {
        @Override public boolean onButtonPress(boolean rightclick) { JourneyNetwork.requestInventoryScan(); return true; }
        @Override public String getButtonTip() { return JourneyButtonPresentation.scanTooltip(); }
    };

    private final Button debugToolButton = new Button("T") {
        @Override public boolean onButtonPress(boolean rightclick) { JourneyNetwork.requestDebugTool(); return true; }
        @Override public String getButtonTip() { return JourneyButtonPresentation.debugToolTooltip(); }
    };

    private boolean visible;
    private boolean newestVisible;
    private boolean favouriteVisible;
    private boolean creativeVisible;
    private boolean deleteVisible;
    private boolean scanVisible;
    private boolean debugToolVisible;

    @Override
    public void onPreDraw(GuiContainer gui) {
        int width = ItemPanels.itemPanel.w;
        visible = ItemPanels.itemPanel.pagePrev != null && JourneyButtonPresentation.researchVisible(width);
        newestVisible = visible && JourneyButtonPresentation.newestVisible(width);
        favouriteVisible = visible && JourneyButtonPresentation.favouriteVisible(width);
        creativeVisible = visible && JourneyButtonPresentation.creativeVisible(width);
        deleteVisible = visible && JourneyButtonPresentation.deleteVisible(width);
        scanVisible = visible && JourneyButtonPresentation.scanVisible(width);
        debugToolVisible = visible && JourneyButtonPresentation.debugToolVisible(width);
        if (!visible) return;
        place(researchButton, 18);
        if (newestVisible) place(newestButton, 36);
        if (favouriteVisible) place(favouriteButton, 54);
        if (creativeVisible) place(creativeButton, 72);
        if (deleteVisible) place(deleteButton, 90);
        if (scanVisible) place(scanButton, 108);
        if (debugToolVisible) place(debugToolButton, 126);
    }

    private static void place(Button button, int offset) {
        button.x = ItemPanels.itemPanel.pagePrev.x + offset;
        button.y = ItemPanels.itemPanel.pagePrev.y;
        button.w = 16;
        button.h = 16;
    }

    @Override public void renderObjects(GuiContainer gui, int mousex, int mousey) {
        if (visible) researchButton.draw(mousex, mousey);
        if (newestVisible) newestButton.draw(mousex, mousey);
        if (favouriteVisible) favouriteButton.draw(mousex, mousey);
        if (creativeVisible) creativeButton.draw(mousex, mousey);
        if (deleteVisible) deleteButton.draw(mousex, mousey);
        if (scanVisible) scanButton.draw(mousex, mousey);
        if (debugToolVisible) debugToolButton.draw(mousex, mousey);
    }

    @Override public void postRenderObjects(GuiContainer gui, int mousex, int mousey) {}
    @Override public void renderSlotUnderlay(GuiContainer gui, Slot slot) {}
    @Override public void renderSlotOverlay(GuiContainer gui, Slot slot) {}

    @Override public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int mouseButton) {
        if (click(visible, researchButton, mousex, mousey, mouseButton)) return true;
        if (click(newestVisible, newestButton, mousex, mousey, mouseButton)) return true;
        if (click(favouriteVisible, favouriteButton, mousex, mousey, mouseButton)) return true;
        if (click(creativeVisible, creativeButton, mousex, mousey, mouseButton)) return true;
        if (click(deleteVisible, deleteButton, mousex, mousey, mouseButton)) return true;
        if (click(scanVisible, scanButton, mousex, mousey, mouseButton)) return true;
        return click(debugToolVisible, debugToolButton, mousex, mousey, mouseButton);
    }

    private static boolean click(boolean shown, Button button, int x, int y, int mouseButton) {
        if (!shown || !button.contains(x, y)) return false;
        button.handleClick(x, y, mouseButton);
        return true;
    }

    @Override public List<String> handleTooltip(GuiContainer gui, int mousex, int mousey, List<String> currenttip) {
        if (visible) researchButton.handleTooltip(mousex, mousey, currenttip);
        if (newestVisible) newestButton.handleTooltip(mousex, mousey, currenttip);
        if (favouriteVisible) favouriteButton.handleTooltip(mousex, mousey, currenttip);
        if (creativeVisible) creativeButton.handleTooltip(mousex, mousey, currenttip);
        if (deleteVisible) deleteButton.handleTooltip(mousex, mousey, currenttip);
        if (scanVisible) scanButton.handleTooltip(mousex, mousey, currenttip);
        if (debugToolVisible) debugToolButton.handleTooltip(mousex, mousey, currenttip);
        return currenttip;
    }

    @Override public List<String> handleItemDisplayName(GuiContainer gui, ItemStack itemstack, List<String> currenttip) { return currenttip; }
    @Override public List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int mousex, int mousey, List<String> currenttip) { return currenttip; }
    @Override public Map<String, String> handleHotkeys(GuiContainer gui, int mousex, int mousey, Map<String, String> hotkeys) { return hotkeys; }
    @Override public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) { return false; }
    @Override public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}
    @Override public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyID) { return false; }
    @Override public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int mouseButton) {}
    @Override public void onMouseUp(GuiContainer gui, int mousex, int mousey, int mouseButton) {}
    @Override public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) { return false; }
    @Override public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}
    @Override public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int mouseButton, long heldTime) {}
}
