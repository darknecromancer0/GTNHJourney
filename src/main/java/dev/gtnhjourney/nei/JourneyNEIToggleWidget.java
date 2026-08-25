package dev.gtnhjourney.nei;

import java.util.List;
import java.util.Map;

import codechicken.nei.Button;
import codechicken.nei.ItemPanels;
import codechicken.nei.guihook.IContainerDrawHandler;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.config.JourneyConfig;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Small native-looking J/N/D controls in NEI's item-panel header. */
public final class JourneyNEIToggleWidget
    implements IContainerDrawHandler, IContainerInputHandler, IContainerTooltipHandler {

    private final Button researchButton = new Button("J") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.toggle();
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyButtonPresentation
                .researchTooltip(JourneyViewState.mode(), ClientStackMirror.serverOnlyCount());
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.RESEARCHED ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button newestButton = new Button("N") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.toggleNewest();
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyViewState.isNewest() ? "Newest view: recent research only. Click to show all NEI items."
                : "Newest view: show the " + JourneyConfig.newestLimit() + " most recently researched states.";
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.NEWEST ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button deleteButton = new Button("D") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.toggleDelete();
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyViewState.isDelete()
                ? "Delete view: plain left-click removes one exact researched state. Click D to show all NEI items."
                : "Delete view: plain left-click one researched state to forget it. Undo remains available.";
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.DELETE ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private boolean visible;
    private boolean newestVisible;
    private boolean deleteVisible;

    @Override
    public void onPreDraw(GuiContainer gui) {
        visible = ItemPanels.itemPanel.pagePrev != null
            && JourneyButtonPresentation.researchVisible(ItemPanels.itemPanel.w);
        newestVisible = false;
        deleteVisible = false;
        if (!visible) return;
        researchButton.x = ItemPanels.itemPanel.pagePrev.x + 18;
        researchButton.y = ItemPanels.itemPanel.pagePrev.y;
        researchButton.w = 16;
        researchButton.h = 16;
        newestVisible = JourneyButtonPresentation.newestVisible(ItemPanels.itemPanel.w);
        if (newestVisible) {
            newestButton.x = ItemPanels.itemPanel.pagePrev.x + 36;
            newestButton.y = ItemPanels.itemPanel.pagePrev.y;
            newestButton.w = 16;
            newestButton.h = 16;
        }
        deleteVisible = JourneyButtonPresentation.deleteVisible(ItemPanels.itemPanel.w);
        if (deleteVisible) {
            deleteButton.x = ItemPanels.itemPanel.pagePrev.x + 54;
            deleteButton.y = ItemPanels.itemPanel.pagePrev.y;
            deleteButton.w = 16;
            deleteButton.h = 16;
        }
    }

    @Override
    public void renderObjects(GuiContainer gui, int mousex, int mousey) {
        if (visible) researchButton.draw(mousex, mousey);
        if (newestVisible) newestButton.draw(mousex, mousey);
        if (deleteVisible) deleteButton.draw(mousex, mousey);
    }

    @Override public void postRenderObjects(GuiContainer gui, int mousex, int mousey) {}
    @Override public void renderSlotUnderlay(GuiContainer gui, Slot slot) {}
    @Override public void renderSlotOverlay(GuiContainer gui, Slot slot) {}

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int mouseButton) {
        if (visible && researchButton.contains(mousex, mousey)) {
            researchButton.handleClick(mousex, mousey, mouseButton);
            return true;
        }
        if (newestVisible && newestButton.contains(mousex, mousey)) {
            newestButton.handleClick(mousex, mousey, mouseButton);
            return true;
        }
        if (deleteVisible && deleteButton.contains(mousex, mousey)) {
            deleteButton.handleClick(mousex, mousey, mouseButton);
            return true;
        }
        return false;
    }

    @Override
    public List<String> handleTooltip(GuiContainer gui, int mousex, int mousey, List<String> currenttip) {
        if (visible) researchButton.handleTooltip(mousex, mousey, currenttip);
        if (newestVisible) newestButton.handleTooltip(mousex, mousey, currenttip);
        if (deleteVisible) deleteButton.handleTooltip(mousex, mousey, currenttip);
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
