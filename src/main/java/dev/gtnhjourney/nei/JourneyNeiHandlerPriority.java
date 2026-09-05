package dev.gtnhjourney.nei;

import codechicken.nei.guihook.GuiContainerManager;

/** Keeps Journey's floating header above NEI regardless of plugin/load registration order. */
final class JourneyNeiHandlerPriority {

    private JourneyNeiHandlerPriority() {}

    static void ensure(JourneyNEIToggleWidget widget) {
        if (widget == null) return;

        if (GuiContainerManager.drawHandlers.peekLast() != widget) {
            GuiContainerManager.drawHandlers.remove(widget);
            GuiContainerManager.drawHandlers.addLast(widget);
        }

        if (GuiContainerManager.inputHandlers.peekFirst() != widget) {
            GuiContainerManager.inputHandlers.remove(widget);
            GuiContainerManager.inputHandlers.addFirst(widget);
        }

        if (GuiContainerManager.tooltipHandlers.peekLast() != widget) {
            GuiContainerManager.tooltipHandlers.remove(widget);
            GuiContainerManager.tooltipHandlers.addLast(widget);
        }
    }
}
