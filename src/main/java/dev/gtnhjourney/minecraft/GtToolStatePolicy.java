package dev.gtnhjourney.minecraft;

import net.minecraft.item.ItemStack;

/** Narrow ownership check for GT5U generated tools whose GT.ToolStats Damage/Mode are runtime-use state. */
public final class GtToolStatePolicy {
    private static final Class<?> META_GENERATED_TOOL = load("gregtech.api.items.MetaGeneratedTool");

    private GtToolStatePolicy() {}

    public static boolean isVerifiedTool(ItemStack stack) {
        return stack != null && stack.getItem() != null && META_GENERATED_TOOL != null
            && META_GENERATED_TOOL.isInstance(stack.getItem());
    }

    public static boolean isRuntimeAvailable() {
        return META_GENERATED_TOOL != null;
    }

    private static Class<?> load(String name) {
        try { return Class.forName(name, false, GtToolStatePolicy.class.getClassLoader()); }
        catch (ClassNotFoundException ignored) { return null; }
        catch (LinkageError ignored) { return null; }
    }
}
