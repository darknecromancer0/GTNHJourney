package dev.gtnhjourney.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerTooltipHandler;

/** Keeps Journey's floating header above NEI regardless of plugin/load registration order. */
final class JourneyNeiHandlerPriority {

    private JourneyNeiHandlerPriority() {}

    static void ensure(GuiContainer gui, JourneyNEIToggleWidget widget) {
        if (widget == null) return;

        if (GuiContainerManager.drawHandlers.peekLast() != widget) {
            GuiContainerManager.drawHandlers.remove(widget);
            GuiContainerManager.drawHandlers.addLast(widget);
        }

        if (GuiContainerManager.inputHandlers.peekFirst() != widget) {
            GuiContainerManager.inputHandlers.remove(widget);
            GuiContainerManager.inputHandlers.addFirst(widget);
        }

        ensureTooltipLast(GuiContainerManager.tooltipHandlers, widget);

        GuiContainerManager manager = gui == null ? null : GuiContainerManager.getManager(gui);
        if (manager != null) ensureTooltipLast(manager.instanceTooltipHandlers, widget);
    }

    static void ensureTooltipLast(List<IContainerTooltipHandler> handlers, JourneyNEIToggleWidget widget) {
        if (handlers == null || widget == null) return;
        synchronized (handlers) {
            int size = handlers.size();
            if (size > 0 && handlers.get(size - 1) == widget) return;
            handlers.remove(widget);
            handlers.add(widget);
        }
    }
}
