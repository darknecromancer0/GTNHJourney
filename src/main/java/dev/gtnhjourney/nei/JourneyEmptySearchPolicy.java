package dev.gtnhjourney.nei;

import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import codechicken.nei.recipe.StackInfo;

/** Narrow correction for the literal unprefixed `empty` search token. */
final class JourneyEmptySearchPolicy {

    private JourneyEmptySearchPolicy() {}

    static boolean resolveSearchMatch(String rawSearch, ItemStack display, boolean nativeSearchMatched) {
        if (!isLiteralEmptyQuery(rawSearch) || display == null || display.getItem() == null) {
            return nativeSearchMatched;
        }

        FluidState fluidState = fluidState(display);
        return resolveLiteralEmptyState(
            nativeSearchMatched,
            fluidState == FluidState.FILLED,
            fluidState == FluidState.EMPTY_CONTAINER,
            visibleNameContainsEmpty(display));
    }

    static boolean resolveLiteralEmptyState(
        boolean nativeSearchMatched,
        boolean filled,
        boolean emptyContainer,
        boolean visibleNameContainsEmpty) {
        if (filled) return visibleNameContainsEmpty && nativeSearchMatched;
        if (emptyContainer) return true;
        return nativeSearchMatched;
    }

    static boolean isLiteralEmptyQuery(String rawSearch) {
        if (rawSearch == null) return false;
        String value = rawSearch.trim().toLowerCase(Locale.ROOT);
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return "empty".equals(value);
    }

    private static FluidState fluidState(ItemStack stack) {
        try {
            FluidStack fluid = StackInfo.getFluid(stack);
            if (fluid != null && fluid.getFluid() != null && fluid.amount > 0) return FluidState.FILLED;
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}

        try {
            if (FluidContainerRegistry.isFilledContainer(stack)) return FluidState.FILLED;
            if (FluidContainerRegistry.isEmptyContainer(stack)) return FluidState.EMPTY_CONTAINER;
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}

        try {
            if (stack.getItem() instanceof IFluidContainerItem) {
                IFluidContainerItem container = (IFluidContainerItem) stack.getItem();
                FluidStack fluid = container.getFluid(stack);
                if (fluid != null && fluid.getFluid() != null && fluid.amount > 0) return FluidState.FILLED;
                if (container.getCapacity(stack) > 0) return FluidState.EMPTY_CONTAINER;
            }
        } catch (RuntimeException ignored) {
        } catch (LinkageError ignored) {}

        // GTNH has a few legacy fluid containers that expose state through NEI/StackInfo rather than Forge's two
        // canonical container APIs. This fallback is deliberately narrow and is considered only after proving the
        // stack has no FluidStack.
        String identity = safeIdentity(stack);
        if (identity.contains("fluidcell") || identity.contains("fluid cell") || identity.contains("fluidcontainer")
            || identity.contains("fluid container") || identity.contains("canister") || identity.contains("capsule")) {
            return FluidState.EMPTY_CONTAINER;
        }
        return FluidState.NOT_FLUID_CONTAINER;
    }

    private static boolean visibleNameContainsEmpty(ItemStack stack) {
        try {
            String name = stack.getDisplayName();
            return name != null && name.toLowerCase(Locale.ROOT).contains("empty");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String safeIdentity(ItemStack stack) {
        StringBuilder out = new StringBuilder();
        try {
            if (stack.getDisplayName() != null) out.append(stack.getDisplayName()).append(' ');
        } catch (RuntimeException ignored) {}
        try {
            if (stack.getItem().getUnlocalizedName(stack) != null) out.append(stack.getItem().getUnlocalizedName(stack)).append(' ');
        } catch (RuntimeException ignored) {}
        try {
            if (stack.getItem().delegate.name() != null) out.append(stack.getItem().delegate.name());
        } catch (RuntimeException ignored) {}
        return out.toString().toLowerCase(Locale.ROOT).replace("_", "").replace(".", "");
    }

    private enum FluidState {
        NOT_FLUID_CONTAINER,
        EMPTY_CONTAINER,
        FILLED
    }
}
