package dev.gtnhjourney.nei;

import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import codechicken.nei.Button;
import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.guihook.IContainerDrawHandler;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.network.JourneyNetwork;

/** Compact NEI/J/F/C/D + L/Group/Order header with S/T anchored beside native NEI G. */
public final class JourneyNEIToggleWidget
    implements IContainerDrawHandler, IContainerInputHandler, IContainerTooltipHandler {

    private final Button neiButton = new Button("NEI") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.setMode(JourneyViewState.Mode.ALL);
            return true;
        }

        @Override
        public String getButtonTip() {
            return "Native NEI view. Journey sorting may still be applied; item clicks remain native NEI behavior.";
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.ALL ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button researchButton = new Button("J") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.setMode(JourneyViewState.Mode.RESEARCHED);
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyButtonPresentation.researchTooltip(JourneyViewState.mode(), ClientStackMirror.serverOnlyCount());
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.RESEARCHED ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button favouriteButton = new Button("F") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.setMode(JourneyViewState.Mode.FAVOURITE);
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyButtonPresentation.favouriteTooltip(JourneyViewState.isFavourite());
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.FAVOURITE ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button creativeButton = new Button("C") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.setMode(JourneyViewState.Mode.CREATIVE);
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyButtonPresentation.creativeTooltip(JourneyViewState.isCreative());
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.CREATIVE ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button deleteButton = new Button("D") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyViewState.setMode(JourneyViewState.Mode.DELETE);
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyViewState.isDelete()
                ? "Delete view: plain left-click removes one exact researched state."
                : "Delete view: left-click one researched state to forget it. Undo remains available.";
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneyViewState.mode() == JourneyViewState.Mode.DELETE ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final Button latestButton = new Button("L") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneySortState.toggleLatest(JourneyViewState.mode());
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneySortState.latest(JourneyViewState.mode())
                ? "Latest priority: ON. Recent activity moves items or whole active groups toward the front."
                : "Latest priority: OFF. Click to prioritize recent activity without changing Group or Order.";
        }

        @Override
        public void draw(int mousex, int mousey) {
            state = JourneySortState.latest(JourneyViewState.mode()) ? 1 : 0;
            super.draw(mousex, mousey);
        }
    };

    private final JourneySortDropdown groupDropdown = new JourneySortDropdown(JourneySortDropdown.Kind.GROUP);
    private final JourneySortDropdown orderDropdown = new JourneySortDropdown(JourneySortDropdown.Kind.ORDER);

    private final Button scanButton = new Button("S") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyNetwork.requestInventoryScan();
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyButtonPresentation.scanTooltip();
        }
    };

    private final Button debugToolButton = new Button("T") {
        @Override
        public boolean onButtonPress(boolean rightclick) {
            JourneyNetwork.requestDebugTool();
            return true;
        }

        @Override
        public String getButtonTip() {
            return JourneyButtonPresentation.debugToolTooltip();
        }
    };

    private boolean visible;
    private boolean rightControlsVisible;
    private boolean scanVisible;
    private boolean debugToolVisible;
    private JourneyHeaderLayout.Layout layout;

    @Override
    public void onPreDraw(GuiContainer gui) {
        visible = ItemPanels.itemPanel.pagePrev != null;
        rightControlsVisible = false;
        scanVisible = false;
        debugToolVisible = false;

        if (!visible) {
            layout = null;
            groupDropdown.close();
            orderDropdown.close();
            return;
        }

        JourneyHeaderLayout.LeftLayout left = JourneyHeaderLayout.left(
            ItemPanels.itemPanel.pagePrev.x,
            ItemPanels.itemPanel.pagePrev.y,
            ItemPanels.itemPanel.pagePrev.w);
        place(neiButton, left.nei);
        place(researchButton, left.researched);
        place(favouriteButton, left.favourite);
        place(creativeButton, left.creative);
        place(deleteButton, left.delete);

        if (ItemPanels.itemPanel.pageNext == null) {
            layout = null;
            groupDropdown.close();
            orderDropdown.close();
            return;
        }

        layout = JourneyHeaderLayout.layout(
            ItemPanels.itemPanel.pagePrev.x,
            ItemPanels.itemPanel.pagePrev.y,
            ItemPanels.itemPanel.pagePrev.w,
            ItemPanels.itemPanel.pageNext.x,
            ItemPanels.itemPanel.pageNext.w);
        rightControlsVisible = true;
        place(latestButton, layout.latest);
        groupDropdown.place(layout.group);
        orderDropdown.place(layout.order);

        scanVisible = layout.scanVisible;
        debugToolVisible = layout.debugVisible;
        if (scanVisible) place(scanButton, layout.scan);
        if (debugToolVisible) place(debugToolButton, layout.debug);
    }

    private static void place(Button button, JourneyHeaderLayout.Slot slot) {
        button.x = slot.x;
        button.y = slot.y;
        button.w = slot.w;
        button.h = slot.h;
    }

    @Override
    public void renderObjects(GuiContainer gui, int mousex, int mousey) {}

    @Override
    public void postRenderObjects(GuiContainer gui, int mousex, int mousey) {
        if (!visible) return;

        NEIClientUtils.gl2DRenderContext(() -> {
            neiButton.draw(mousex, mousey);
            researchButton.draw(mousex, mousey);
            favouriteButton.draw(mousex, mousey);
            creativeButton.draw(mousex, mousey);
            deleteButton.draw(mousex, mousey);

            if (!rightControlsVisible) return;

            latestButton.draw(mousex, mousey);
            if (scanVisible) scanButton.draw(mousex, mousey);
            if (debugToolVisible) debugToolButton.draw(mousex, mousey);
            groupDropdown.drawMain(mousex, mousey);
            orderDropdown.drawMain(mousex, mousey);

            // Popup options are deliberately last so no Journey or native NEI item cell can cover them.
            groupDropdown.drawOverlay(mousex, mousey);
            orderDropdown.drawOverlay(mousex, mousey);
        });
    }

    @Override
    public void renderSlotUnderlay(GuiContainer gui, Slot slot) {}

    @Override
    public void renderSlotOverlay(GuiContainer gui, Slot slot) {}

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int mouseButton) {
        if (!visible) return false;
        if (rightControlsVisible) {
            if (groupDropdown.isOpen() && consume(groupDropdown.click(mousex, mousey, mouseButton))) return true;
            if (orderDropdown.isOpen() && consume(orderDropdown.click(mousex, mousey, mouseButton))) return true;
            if (consume(groupDropdown.click(mousex, mousey, mouseButton))) return true;
            if (consume(orderDropdown.click(mousex, mousey, mouseButton))) return true;
        }
        if (consume(click(neiButton, mousex, mousey, mouseButton))) return true;
        if (consume(click(researchButton, mousex, mousey, mouseButton))) return true;
        if (consume(click(favouriteButton, mousex, mousey, mouseButton))) return true;
        if (consume(click(creativeButton, mousex, mousey, mouseButton))) return true;
        if (consume(click(deleteButton, mousex, mousey, mouseButton))) return true;
        if (!rightControlsVisible) return false;
        if (consume(click(latestButton, mousex, mousey, mouseButton))) return true;
        if (scanVisible && consume(click(scanButton, mousex, mousey, mouseButton))) return true;
        return debugToolVisible && consume(click(debugToolButton, mousex, mousey, mouseButton));
    }

    private static boolean consume(boolean handled) {
        if (!handled) return false;
        cancelNativeItemClick();
        return true;
    }

    private static void cancelNativeItemClick() {
        if (ItemPanels.itemPanel != null) ItemPanels.itemPanel.mouseDownSlot = -1;
    }

    private static boolean click(Button button, int x, int y, int mouseButton) {
        if (!button.contains(x, y)) return false;
        button.handleClick(x, y, mouseButton);
        return true;
    }

    @Override
    public List<String> handleTooltip(GuiContainer gui, int mousex, int mousey, List<String> currenttip) {
        if (!visible) return currenttip;
        neiButton.handleTooltip(mousex, mousey, currenttip);
        researchButton.handleTooltip(mousex, mousey, currenttip);
        favouriteButton.handleTooltip(mousex, mousey, currenttip);
        creativeButton.handleTooltip(mousex, mousey, currenttip);
        deleteButton.handleTooltip(mousex, mousey, currenttip);
        if (!rightControlsVisible) return currenttip;
        latestButton.handleTooltip(mousex, mousey, currenttip);
        groupDropdown.tooltip(mousex, mousey, currenttip);
        orderDropdown.tooltip(mousex, mousey, currenttip);
        if (scanVisible) scanButton.handleTooltip(mousex, mousey, currenttip);
        if (debugToolVisible) debugToolButton.handleTooltip(mousex, mousey, currenttip);
        return currenttip;
    }

    @Override
    public List<String> handleItemDisplayName(GuiContainer gui, ItemStack itemstack, List<String> currenttip) {
        return currenttip;
    }

    @Override
    public List<String> handleItemTooltip(
        GuiContainer gui,
        ItemStack itemstack,
        int mousex,
        int mousey,
        List<String> currenttip) {
        return currenttip;
    }

    @Override
    public Map<String, String> handleHotkeys(
        GuiContainer gui,
        int mousex,
        int mousey,
        Map<String, String> hotkeys) {
        return hotkeys;
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
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int mouseButton) {}

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int mouseButton) {}

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        return false;
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int mouseButton, long heldTime) {}
}
