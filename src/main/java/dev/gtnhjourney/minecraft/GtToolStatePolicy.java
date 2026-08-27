package dev.gtnhjourney.minecraft;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.item.ItemStack;

/** Narrow ownership check for GT5U generated tools whose GT.ToolStats Damage/Mode are runtime-use state. */
public final class GtToolStatePolicy {

    private static final Class<?> META_GENERATED_TOOL = load("gregtech.api.items.MetaGeneratedTool");
    private static final Field TOOL_STATS = field(META_GENERATED_TOOL, "mToolStats");

    private GtToolStatePolicy() {}

    /**
     * A generated tool is verified only when its current GT runtime still knows the tool meta. If the optional runtime
     * cannot expose the registry map, fail open so a future GT update cannot accidentally hide otherwise valid tools.
     */
    public static boolean isVerifiedTool(ItemStack stack) {
        if (!isGeneratedTool(stack)) return false;
        Boolean registered = registeredMeta(stack);
        return registered == null || registered.booleanValue();
    }

    /** Returns true only when the live GT runtime positively proves that this generated-tool meta is not registered. */
    public static boolean isKnownInvalidToolState(ItemStack stack) {
        if (!isGeneratedTool(stack)) return false;
        Boolean registered = registeredMeta(stack);
        return registered != null && !registered.booleanValue();
    }

    public static boolean isRuntimeAvailable() {
        return META_GENERATED_TOOL != null;
    }

    private static boolean isGeneratedTool(ItemStack stack) {
        return stack != null && stack.getItem() != null
            && META_GENERATED_TOOL != null
            && META_GENERATED_TOOL.isInstance(stack.getItem());
    }

    private static Boolean registeredMeta(ItemStack stack) {
        if (TOOL_STATS == null || stack == null || stack.getItem() == null) return null;
        try {
            Object raw = TOOL_STATS.get(stack.getItem());
            if (!(raw instanceof Map)) return null;
            Map<?, ?> stats = (Map<?, ?>) raw;
            return Boolean.valueOf(stats.containsKey(Short.valueOf((short) stack.getItemDamage())));
        } catch (IllegalAccessException ignored) {
            return null;
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static Field field(Class<?> owner, String name) {
        if (owner == null) return null;
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException ignored) {
            return null;
        } catch (SecurityException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, GtToolStatePolicy.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
