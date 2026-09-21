package dev.gtnhjourney.nei;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerTooltipHandler;

/** Keeps Journey's floating header above NEI regardless of plugin/load registration order. */
final class JourneyNeiHandlerPriority {

    private JourneyNeiHandlerPriority() {}

    static void ensure(GuiContainer gui, JourneyNEIToggleWidget widget, JourneyNEIInputHandler itemInput) {
        if (widget == null || itemInput == null) return;

        if (GuiContainerManager.drawHandlers.peekLast() != widget) {
            GuiContainerManager.drawHandlers.remove(widget);
            GuiContainerManager.drawHandlers.addLast(widget);
        }

        if (GuiContainerManager.inputHandlers.size() < 2
            || GuiContainerManager.inputHandlers.get(0) != widget
            || GuiContainerManager.inputHandlers.get(1) != itemInput) {
            GuiContainerManager.inputHandlers.remove(widget);
            GuiContainerManager.inputHandlers.remove(itemInput);
            // Header owns its overlapping controls first. Journey item semantics must be second so Alt+LMB reaches
            // favourite-add before NEI's native collapsible-group Alt+LMB handler can consume the click.
            GuiContainerManager.inputHandlers.addFirst(itemInput);
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
