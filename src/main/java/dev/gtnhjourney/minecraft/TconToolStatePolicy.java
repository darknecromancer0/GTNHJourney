package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;

/** Ownership check for Tinkers' Construct 1.7.10 ToolCore runtime durability state. */
public final class TconToolStatePolicy {

    private static final Class<?> TOOL_CORE = load("tconstruct.library.tools.ToolCore");

    private TconToolStatePolicy() {}

    public static boolean isVerifiedTool(ItemStack stack) {
        return stack != null && stack.getItem() != null && TOOL_CORE != null && TOOL_CORE.isInstance(stack.getItem());
    }

    public static boolean isRuntimeAvailable() {
        return TOOL_CORE != null;
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, TconToolStatePolicy.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
